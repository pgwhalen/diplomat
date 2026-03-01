# Java Backend `mod.rs` Deduplication Proposal

`tool/src/java/mod.rs` is ~3900 lines. This document catalogs duplication patterns and proposes refactorings, ranked by impact on readability.

---

## 1. "Resolve type_id and format name" pattern (HIGH impact)

**The problem:** The highlighted snippet — `let type_id = ty.id().expect("X must have id"); self.formatter.fmt_type_name(type_id).to_string()` — appears ~30 times across the file, with minor variations in the `expect` message ("enum must have id", "opaque must have id", "struct must have id").

**Occurrences:** Lines 2290-2291, 2293-2294, 2297-2298, 2321-2322, 2324-2325, 2328-2330, 2799-2800, 2803-2804, 2807-2808, 2824-2825, 2828-2829, 2860-2861, 2866-2867, 2872-2873, 2911-2912, 2970-2971, 2974-2975, 3004-3005, 3066-3067, 1093-1094, 1344-1345, 1400-1401, 1405-1406, 1410-1411, 1641-1642, 1646-1647, 1651-1652, 1731, 1741, 1751, 963-964, etc.

**Proposal:** Add a helper method:
```rust
fn fmt_type_name_str<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
    let type_id = ty.id().expect("type must have id");
    self.formatter.fmt_type_name(type_id).to_string()
}
```

**Assessment:** Strong win. Eliminates ~60 lines of boilerplate and makes callsites much more readable. The expect message differences are not useful — if it panics, the backtrace shows which call site it was.

---

## 2. Multiple near-identical "get Java type name from Type" functions (HIGH impact)

**The problem:** There are 5+ functions that all match on a `Type` and return a Java type string, differing only in which type positions they accept and minor details:

| Function | Lines | Position | Notes |
|---|---|---|---|
| `callback_param_java_type` | 2285-2302 | `OutType` | For callback params |
| `callback_param_java_type_input` | 2317-2334 | `InputOnly` | Identical logic to above |
| `field_java_type` | 2795-2843 | generic `P` | Adds Slice + DiplomatOption handling |
| `gen_out_type_java` | 1594-1621 | `OutType` | Adds Optional wrapping for opaque |
| `gen_out_type_java_boxed` | 1624-1631 | `OutType` | Boxed primitives variant |

`callback_param_java_type` and `callback_param_java_type_input` are literally the same logic for different type positions.

**Proposal:** Unify `callback_param_java_type` and `callback_param_java_type_input` into a single generic function:
```rust
fn type_to_java_name<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
    match ty {
        Type::Primitive(prim) => self.formatter.fmt_primitive_as_java(*prim).to_string(),
        Type::Enum(_) | Type::Struct(_) | Type::Opaque(_) => self.fmt_type_name_str(ty),
        _ => "Object".to_string(),
    }
}
```

Then `field_java_type` can call `type_to_java_name` for its base cases and only add the Slice/DiplomatOption branches. `gen_out_type_java` is different enough (Optional wrapping, error reporting) that it should stay separate but can use `fmt_type_name_str` internally.

**Assessment:** Very strong win. Eliminates an entire function (`callback_param_java_type_input`) and simplifies several others. The two `callback_param_java_type*` functions being identical-but-for-position is the most egregious duplication in the file.

---

## 3. `uses_optional` computation (MEDIUM impact)

**The problem:** The logic to determine whether a type definition needs `import java.util.Optional` is copy-pasted across three functions with slight variations:

- `gen_opaque_def` (lines 502-522) — has special handling for iterator/indexer exclusion
- `gen_enum_def` (lines 2357-2370)
- `gen_struct_def` (lines 2457-2470)

The enum and struct versions are identical. The opaque version adds iterator/indexer filtering.

**Proposal:** Extract a helper:
```rust
fn methods_use_optional(&self, methods: &[&Method]) -> bool {
    methods.iter().any(|method| {
        if let ReturnType::Infallible(SuccessType::OutType(Type::Opaque(op))) = &method.output {
            if op.is_optional() { return true; }
        }
        if matches!(&method.output, ReturnType::Nullable(_)) { return true; }
        method.params.iter().any(|p| matches!(&p.ty, Type::Opaque(op) if op.is_optional()))
    })
}
```

The opaque version can pre-filter the methods list to exclude special iterators/indexers before calling.

**Assessment:** Good win. Eliminates ~30 lines of duplication and makes intent clearer at callsites.

---

## 4. Method categorization + callback collection (MEDIUM impact)

**The problem:** `gen_opaque_def` (lines 524-576) and `gen_struct_def` (lines 2472-2536) both:
1. Generate `native_methods` from `supported_methods`
2. Split methods into `constructor_methods`, `companion_methods`, `self_methods`
3. Collect `callback_interfaces`, `callback_runners`, `callback_statics` from all methods

The struct version is almost identical to the opaque version.

**Proposal:** Extract a struct and builder:
```rust
struct CategorizedMethods {
    native_methods: Vec<JavaNativeMethodInfo>,
    constructor_methods: Vec<JavaMethodInfo>,
    companion_methods: Vec<JavaMethodInfo>,
    self_methods: Vec<JavaMethodInfo>,
    all_cb_interfaces: Vec<String>,
    all_cb_runners: Vec<String>,
    all_cb_statics: Vec<String>,
    has_callbacks: bool,
}

fn categorize_methods(&self, methods: &[&Method], type_name: &str, is_error: bool, fields: Option<&[JavaStructFieldInfo]>) -> CategorizedMethods
```

**Assessment:** Moderate win. The code is straightforward, so duplication isn't confusing, but it does save ~50 lines. The risk is that different callers may diverge in future (e.g., enums don't have constructors), so the shared function would need parameters to control behavior. That said, the current code already handles this via `gen_method_no_constructors` for enums.

---

## 5. `field_to_native` vs `field_to_native_value` (MEDIUM impact)

**The problem:** `field_to_native` (lines 2984-3046) and `field_to_native_value` (lines 3050-3101) are nearly identical. The latter is called from the DiplomatOption branch of the former. Both handle Primitive, Opaque, Enum, Struct, and Slice in the same way, with tiny differences:
- `field_to_native` uses `this.{field_name}` while `field_to_native_value` also uses `this.{field_name}`
- `field_to_native` handles DiplomatOption by calling `field_to_native_value` for the inner type
- Some minor formatting differences in the Slice branch

**Proposal:** The current factoring (field_to_native delegates to field_to_native_value for Option inner) is actually already reasonable. But the Slice handling in both functions duplicates string encoding logic. Extract a slice_to_native helper:
```rust
fn slice_to_native(&self, slc: &Slice, field_name: &str, shouty: &str) -> String
```

**Assessment:** Minor win. The current structure is already factored correctly at the top level. The remaining duplication is in the Slice branches, which could share a helper, but the formatting differences make it fiddly.

---

## 6. `field_from_native` duplication in DiplomatOption branch (MEDIUM impact)

**The problem:** `field_from_native` (lines 2847-2955) contains a DiplomatOption branch (lines 2906-2952) that duplicates the Struct and Slice reading logic from its own parent function. E.g., the Struct case at line 2909-2916 is identical to lines 2871-2877, and the Slice cases at 2918-2943 duplicate 2879-2904.

**Proposal:** Refactor `field_from_native` so the DiplomatOption branch recursively calls `field_from_native` for the inner type (or a shared inner helper):
```rust
Type::DiplomatOption(inner) => {
    let vh_is_ok = format!("VH_{shouty}_IS_OK");
    let inner_expr = self.field_from_native_inner(inner.as_ref(), field_name, shouty);
    format!("(boolean) {vh_is_ok}.get(seg, 0L) ? {inner_expr} : null")
}
```

Where `field_from_native_inner` handles the type dispatch but uses the option-specific VH/offset names. This is tricky because the VarHandle names differ (e.g., `VH_{shouty}_VALUE` for option's primitive value vs `VH_{shouty}` for a top-level primitive), but `field_from_native_via_vh` already handles the primitive/enum/opaque cases.

**Assessment:** Moderate win for readability. The DiplomatOption Slice branch is the worst offender — it's a large block that's basically copy-pasted from the non-option Slice branch.

---

## 7. File path generation (LOW-MEDIUM impact)

**The problem:** The expression `format!("src/main/java/{}/{lib_name}/{type_name}.java", self.domain.replace('.', "/"), lib_name = self.lib_name)` appears 4 times (opaque, struct, enum, trait defs).

**Proposal:**
```rust
fn java_file_path(&self, type_name: &str) -> String {
    format!("src/main/java/{}/{}/{type_name}.java",
        self.domain.replace('.', "/"), self.lib_name)
}
```

**Assessment:** Easy win. Small but removes a repeated format string that's easy to get wrong.

---

## 8. FFI layout string mapping (LOW-MEDIUM impact)

**The problem:** Multiple functions map types to their FFI layout strings:
- `get_type_layout` (lines 764-780) — for OutType
- `field_layout_element` (lines 2770-2793) — for generic P, adds Slice/DiplomatOption
- `push_param_layouts` (lines 701-735) — pushes into a Vec, handles Callback/ImplTrait

They share the Primitive/Opaque/Enum/Struct core logic.

**Proposal:** Extract a core `type_to_ffi_layout`:
```rust
fn type_to_ffi_layout<P: hir::TyPosition>(&self, ty: &Type<P>) -> Option<String> {
    match ty {
        Type::Primitive(prim) => Some(self.formatter.fmt_primitive_as_ffi(*prim).to_string()),
        Type::Opaque(_) => Some("ValueLayout.ADDRESS".to_string()),
        Type::Enum(_) => Some("ValueLayout.JAVA_INT".to_string()),
        Type::Struct(_) => {
            let type_name = self.fmt_type_name_str(ty);
            Some(format!("{type_name}.LAYOUT"))
        }
        _ => None,
    }
}
```

Then `get_type_layout`, `field_layout_element`, and `push_param_layouts` call this for the base cases and handle their special cases (Slice, Callback, DiplomatOption, etc.) locally.

**Assessment:** Moderate win. Centralizes the core mapping. However, the special cases differ significantly between callers, so the helper only covers ~4 match arms.

---

## 9. Size/align function chain (LOW impact)

**The problem:** There's a chain of 6 size/align functions:
- `out_type_size_align` → calls `field_size_align_generic`
- `type_size_align_by_id` → resolves TypeId, then matches
- `struct_size_align_by_id` → alias for `type_size_align_by_id`
- `field_size_align_generic` → the actual implementation
- `get_out_type_size_align` (method) → calls `out_type_size_align`
- `get_field_size_align` (method) → calls `field_size_align_generic`

`out_type_size_align` is just `field_size_align_generic` with a type alias. `struct_size_align_by_id` is explicitly labeled "Alias for backward compat". The two methods are thin wrappers.

**Proposal:** Remove `out_type_size_align`, `struct_size_align_by_id`, `get_out_type_size_align`, and `get_field_size_align`. Replace all calls with `field_size_align_generic(ty, self.formatter)` directly or make `field_size_align_generic` a method on `ItemGenContext`.

**Assessment:** Small win. The indirection layers add cognitive load for anyone reading the code.  Making `field_size_align_generic` a method (`self.field_size_align(ty)`) would be the cleanest.

---

## 10. `callback_param_method_type_class` vs `callback_param_method_type_class_input` (LOW-MEDIUM impact)

**The problem:** `callback_param_method_type_class` (lines 1984-1993, takes `OutType`) and `callback_param_method_type_class_input` (lines 1972-1981, takes `Type<InputOnly>`) are identical logic for different type positions.

**Proposal:** Unify into a single generic:
```rust
fn type_to_method_type_class<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
    match ty {
        Type::Primitive(prim) => format!("{}.class", self.formatter.fmt_primitive_as_java(*prim)),
        Type::Enum(_) => "int.class".to_string(),
        Type::Struct(_) | Type::Opaque(_) => "MemorySegment.class".to_string(),
        _ => "MemorySegment.class".to_string(),
    }
}
```

**Assessment:** Easy win. Same situation as #2 — two functions that exist only because of type position differences.

---

## 11. `gen_result_value_extract` vs `wrap_invoke_result` (LOW impact)

**The problem:** `gen_result_value_extract` (lines 1392-1418) extracts a value at offset 0 from a result MemorySegment. `wrap_invoke_result` (lines 1634-1659) wraps an invoke call with type casting. They share the same type-dispatching structure but produce different expressions:
- `gen_result_value_extract`: reads from a segment (`seg.get(layout, 0L)`, `new Foo(seg.get(ADDRESS, 0L))`)
- `wrap_invoke_result`: casts an invoke call (`(type) invokeCall`, `new Foo((MemorySegment) invokeCall)`)

**Proposal:** These are semantically different operations (read-from-segment vs cast-invoke-result). The structural similarity is coincidental — they both dispatch on type variant but do different things.

**Assessment:** Not recommended. Forcing them together would obscure their different purposes. The pattern is "match on type variant and do something" which is inherent to code generation.

---

## 12. Test helper boilerplate (LOW impact)

**The problem:** `gen_opaque_for_test`, `gen_struct_for_test`, `gen_all_for_test`, `gen_all_with_traits_for_test` (lines 3114-3772) all share the same setup:
```rust
let tcx = new_tcx(tk_stream);
let docs_urls = std::collections::HashMap::new();
let docs_generator = &diplomat_core::hir::DocsUrlGenerator::with_base_urls(None, docs_urls);
let formatter = JavaFormatter::new(&tcx, docs_generator);
let errors = ErrorStore::default();
let cx = ItemGenContext { tcx: &tcx, formatter: &formatter, errors: &errors, ... };
```

They differ only in which types they iterate over (opaque only, struct only, all types, all types + traits).

**Proposal:** Extract a setup helper and a generic iteration helper:
```rust
fn with_test_context(tk_stream: proc_macro2::TokenStream, f: impl FnOnce(&ItemGenContext, &TypeContext) -> String) -> String {
    let tcx = new_tcx(tk_stream);
    // ... setup ...
    f(&cx, &tcx)
}
```

**Assessment:** Minor win. It's test code, so readability matters less. But the boilerplate is noisy enough that it obscures what each test actually tests. I'd do it mostly to make tests easier to write going forward.

---

## 13. `gen_trait_def` method body construction (LOW impact)

**The problem:** `gen_trait_def` (lines 2011-2216) manually builds a Java source file by appending strings, while all other type defs use Askama templates. It has some internal duplication in building VarHandles, statics, etc.

**Proposal:** Create a `Trait.java.jinja` template like the other type definitions have. This would:
- Eliminate the manual string building (~100 lines)
- Make the trait file structure visible at a glance
- Be consistent with opaque/struct/enum which all use templates

**Assessment:** High readability win, but it's a larger refactoring (new template file). The current code works, but is harder to read and maintain than the template-based approach. This is more of an architectural improvement than a deduplication.

---

## 14. `gen_callback_statics` vs trait MH/upcall initialization (LOW impact)

**The problem:** `gen_callback_statics` (lines 1895-1946) and the trait statics initialization in `gen_trait_def` (lines 2148-2164) both generate nearly identical MethodHandle lookup + upcall stub creation code.

**Proposal:** Extract a helper:
```rust
fn gen_mh_upcall_static(&self, mh_name: &str, upcall_name: &str, class_name: &str, runner_name: &str, mt_return: &str, mt_params: &[String], fd: &str) -> String
```

**Assessment:** Minor win. The format strings are complex enough that a shared helper would reduce risk of them diverging, but the contexts differ (callback statics are class-level fields with static initializers; trait statics are inside a `Statics` inner class).

---

## Summary: Recommended changes by priority

### Do first (high impact, low risk):
1. **#1: `fmt_type_name_str` helper** — Eliminates ~30 call sites of 2-line boilerplate
2. **#2: Unify `callback_param_java_type` / `_input` + introduce `type_to_java_name`** — Eliminates an entire duplicate function
3. **#10: Unify `callback_param_method_type_class` / `_input`** — Same pattern as #2

### Do second (medium impact):
4. **#7: `java_file_path` helper** — Trivial, safe
5. **#3: `methods_use_optional` helper** — Eliminates 3x copy-pasted block
6. **#9: Flatten size/align chain** — Remove unnecessary indirection layers
7. **#8: `type_to_ffi_layout` core helper** — Centralizes a 4-arm match repeated 3 times

### Consider (moderate impact, more effort):
8. **#4: `CategorizedMethods` struct** — Reduces opaque+struct def boilerplate, but adds abstraction
9. **#6: DiplomatOption `field_from_native` dedup** — Tricky due to VarHandle naming differences
10. **#13: Trait template** — Architectural consistency improvement

### Skip (low impact or not recommended):
11. **#5: `field_to_native` / `field_to_native_value`** — Already factored reasonably
12. **#11: `gen_result_value_extract` vs `wrap_invoke_result`** — Structural similarity is coincidental
13. **#12: Test helper boilerplate** — It's test code; nice to have but low priority
14. **#14: MH/upcall static generation** — Contexts differ enough that sharing is awkward

---

## Progress

All "Do first" and "Do second" items have been implemented. File reduced from ~3913 to ~3762 lines (-151 lines net, including new helper methods).

### Completed:
1. **#1: `fmt_type_name_str` helper** — Added. ~30 call sites replaced with one-liner instead of 2-line id+format pattern.
2. **#2: `type_to_java_name` generic** — Added. Replaced both `callback_param_java_type` and `callback_param_java_type_input` (deleted). Also used by `field_java_type` for its base cases.
3. **#10: `type_to_method_type_class` generic** — Added. Replaced both `callback_param_method_type_class` and `callback_param_method_type_class_input` (deleted).
4. **#7: `java_file_path` helper** — Added. Replaced 4 call sites (opaque, struct, enum, trait defs).
5. **#3: `methods_use_optional` helper** — Added. Replaced 3 copy-pasted blocks in `gen_opaque_def`, `gen_enum_def`, `gen_struct_def`. Opaque version pre-filters iterator/indexer methods before calling.
6. **#9: Flatten size/align chain** — `field_size_align` and `type_size_align_by_id` are now methods on `ItemGenContext`. Removed 5 functions/wrappers: `out_type_size_align`, `type_size_align_by_id` (free fn), `struct_size_align_by_id`, `get_out_type_size_align`, `get_field_size_align`.
7. **#8: `type_to_ffi_layout` helper** — Added. Simplified `get_type_layout`, `push_param_layouts`, and `field_layout_element` to delegate base cases.

### Not implemented (per proposal recommendations):
- **#4**: `CategorizedMethods` struct — moderate effort, risk of over-abstraction
- **#5**: `field_to_native` / `field_to_native_value` — already factored reasonably
- **#6**: `field_from_native` DiplomatOption dedup — tricky VarHandle naming differences
- **#11**: `gen_result_value_extract` vs `wrap_invoke_result` — not recommended
- **#12**: Test helper boilerplate — low priority
- **#13**: Trait template — architectural change, larger effort
- **#14**: MH/upcall static generation — contexts differ
