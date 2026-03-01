# Java Backend for Diplomat

This document tracks the effort to build a Java backend for Diplomat. When implementing any new feature, consult `book/src/developer.md` for the recommended approach to building and testing backend features incrementally.

## Approach

The Java backend uses the **Java FFM (Foreign Function & Memory) API** (`java.lang.foreign.*`) rather than JNI. This requires JDK 22+. Opaque types implement `AutoCloseable` for deterministic native memory cleanup.

## Key Files

- `tool/src/java/mod.rs` — backend entry point (`run()`, `attr_support()`, `ItemGenContext` codegen)
- `tool/src/java/formatter.rs` — Java name/type formatting, keyword avoidance
- `tool/templates/java/Opaque.java.jinja` — Askama template for opaque type classes
- `tool/templates/java/Struct.java.jinja` — Askama template for struct type classes
- `tool/templates/java/Lib.java.jinja` — `DiplomatLib.java` runtime support (write buffer, string view layout)
- `example/java/somelib/` — Gradle-based example project (JDK 25, JUnit 5)
- `example/config.toml` — `[java]` section with `domain` and `dylib-name`
- `feature_tests/java/somelib/` — Gradle-based feature test project (JDK 25, JUnit 5)
- `tool/src/java/snapshots/` — insta snapshot tests

## Commands

```bash
cargo make gen-java-example    # Regenerate Java bindings for example/
cargo make gen-java-feature    # Regenerate Java bindings for feature_tests/
cargo make test-java-example   # Build native lib + run Gradle tests
cargo make test-java-feature   # Build native lib + run Gradle feature tests
cargo make test-java           # Run both example and feature tests
cargo test -p diplomat-tool -- java::test   # Run Java backend unit/snapshot tests (28 tests)
```

## Current Status

### What works

- **Opaque types** with `AutoCloseable` / destroy via FFM downcall handles
- **Constructors** — `#[diplomat::attr(auto, constructor)]` generates idiomatic `new TypeName(...)` Java constructors
  - Infallible constructors for opaques (assign `this.handle`) and structs (assign fields)
  - Fallible constructors (throw on error path)
  - Named constructors (`#[diplomat::attr(auto, named_constructor = "...")]`) generate static factory methods
- **Static factory methods** (non-constructor methods returning `Box<Self>`)
- **Instance methods** with `&self` (opaque and struct)
- **Primitive parameters and returns** (all integer sizes, float, double, boolean, char)
- **Opaque parameters and returns** (passed as `MemorySegment`)
- **`&DiplomatStr` parameters** (Java `String` converted to UTF-8 bytes via `Arena`)
- **`&DiplomatStr16` parameters** (Java `String` converted to UTF-16 char array via `Arena`)
- **Enum types** — generated as proper Java `enum` classes with `toNative()`/`fromNative()` methods
  - Enum parameters and returns use the generated enum type
  - Enum self methods pass `this.toNative()`
  - Error enums (`#[diplomat::attr(auto, error)]`) generate a companion `*Exception` class
- **Structs** — generated as POJOs with FFM `StructLayout`, `fromNative`/`toNative` conversion, C ABI padding
  - Primitive fields (all sizes), nested struct fields, boolean fields, enum fields, slice fields (str/primitive), `DiplomatOption<T>` fields
  - Structs as method parameters (passed by value via `toNative`)
  - Structs as return types (received via `SegmentAllocator` + `fromNative`)
  - Struct self methods (consuming `self` passed by value)
- **Fallible returns** (`Result<T, E>`) — result layout with `is_ok` discriminant, success extracted or error thrown
  - Unit errors (`Result<T, ()>`) → `throw new RuntimeException("Diplomat error")`
  - Struct errors (`Result<T, ErrorStruct>`) → struct `extends RuntimeException`, thrown directly
  - Opaque errors (`Result<T, Box<ErrorOpaque>>`) → opaque `extends RuntimeException`, thrown directly
  - Enum errors (`Result<T, ErrorEnum>`) → thrown as `*Exception` (if `#[diplomat::attr(auto, error)]`) or `RuntimeException` with enum value
  - Primitive error values (`Result<(), i32>`) → thrown as `RuntimeException` with value in message
  - All success types: `Unit`, `OutType` (opaque/struct/primitive), `Write` (string)
- **Nullable returns** (`Option<T>`) — wrapped in `Optional<T>` with boxed primitives
- **Write returns** (`DiplomatWrite` / stringifiers) — write buffer allocated, converted to `String`
- **Fallible + Write combination** (`Result<String, E>`) — write buffer destroyed on error path
- **Optional opaque parameters** — nullable `MemorySegment` passed as `MemorySegment.NULL` when absent
- **Optional opaque returns** — `Optional<T>` wrapping null-checked `MemorySegment`
- **Error types** — structs and opaques with `#[diplomat::attr(auto, error)]` extend `RuntimeException`
- **Method name renaming** via `#[diplomat::attr]`
- **Java keyword collision avoidance**
- **Callbacks** — `impl Fn(args) -> ret` parameters generate `@FunctionalInterface` inner interfaces, static runner methods, and FFM upcall stubs. At each call site, the Java lambda is registered in a `ConcurrentHashMap` registry and a `DiplomatCallback` native struct is allocated with `{data=id, run_callback=stub, destructor=destructor_stub}`.
  - Simple callbacks: `Fn(i32) -> i32`, `Fn()`, `Fn() -> i32`
  - Callbacks with struct params: `Fn(SomeStruct) -> i32`
  - Multiple callback params in one method
  - `CallbackHolder` / `MutableCallbackHolder` (stored callbacks with `'static` lifetime)
  - **Disabled for Java**: callbacks with str/opaque/slice args, result/option/DiplomatResult returns, inner/str/slice/struct-slice conversion, opaque result errors
- **Traits** — Diplomat trait definitions generate Java `interface` files with vtable layouts, a `Statics` inner class for upcall stubs, and a `createNative()` factory method. Users implement the interface and pass it to methods accepting `impl Trait`.
  - Trait methods with primitive, void, and struct params/returns
  - **Disabled for Java**: trait methods returning `Result`
- **JavaDoc documentation** — `/// doc comments` from Rust source are rendered as `/** ... */` JavaDoc blocks on types, methods, struct fields, enum variants, and trait interfaces/methods. Uses `{@link TypeName}` syntax for cross-type references.
- **Feature tests** — `feature_tests/java/somelib/` Gradle project with JUnit 5 tests

### What doesn't work yet

- Struct fields of certain unsupported types (e.g., struct slices, string view slices) — emit `Object` placeholder
- Cyclic struct references in result layouts — circular static class initialization in Java (e.g., `CyclicStructA` ↔ `CyclicStructB` when result layouts create cross-type references)
- Slice parameters/returns other than `&str` / `&DiplomatStr16` — not supported (primitive slices work as struct fields)
- Accessors / comparators
- Java is not yet included in CI meta-tasks (`test-example`, `test-feature`, `test-all`)

### `attr_support()` flags

In `tool/src/java/mod.rs`, the following are set to `true`: `method_overloading`, `utf8_strings`, `utf16_strings`, `non_exhaustive_structs`, `option`, `custom_errors`, `constructors`, `named_constructors`, `fallible_constructors`, `iterators`, `iterables`, `indexing`, `callbacks`, `traits`. All others are `false`. Flags should be flipped to `true` as features are implemented.

## Feature Checklist

Copied from `book/src/developer.md` — check off features as they are added to the Java backend:

- [x] **primitive types**: All integer sizes, float, double, boolean mapped to Java equivalents
- [x] **opaque types**:
  - [x] basic definition
  - [x] return a boxed opaque (with `AutoCloseable` / destroy cleanup)
  - [x] as self parameter
  - [x] as another parameter
- [x] **structs**:
  - [x] basic definition (primitive fields, nested struct fields)
  - [x] as return type
  - [x] as parameter
  - [x] struct self methods
  - [x] enum fields
  - [x] slice fields (str + primitive)
  - [x] `Option<T>` fields
- [x] **enums** (as proper Java enum classes with `toNative()`/`fromNative()`)
- [x] **writeable** (DiplomatWrite / stringifiers)
- [ ] **slices**:
  - [ ] primitive slices
  - [x] str slices (`&DiplomatStr` mapped to Java `String` via UTF-8)
  - [x] str16 slices (`&DiplomatStr16` mapped to Java `String` via UTF-16)
  - [ ] owned slices
  - [ ] slices of strings
  - [ ] strings
- [ ] **borrows** — ensure managed objects aren't cleaned up while something depends on them
  - [ ] borrows of parameters
  - [ ] in struct fields
- [x] **nullables** — returning `Option` types as `Optional<T>`
- [x] **fallibles** — returning `Result` types (discriminated union with error throwing)

## `BackendAttrSupport` Flag Checklist

This section tracks every flag in `BackendAttrSupport` (defined in `core/src/hir/attrs.rs`) and its status for the Java backend. The current `attr_support()` in `tool/src/java/mod.rs` has only `method_overloading` set to `true`; all others default to `false`. Each flag below is categorized as:

- **DONE** — already implemented and set to `true`
- **TODO** — should be implemented for the Java backend
- **WON'T DO** — not appropriate or not possible for Java
- **MIGHT DO** — unclear whether it's worth implementing; decision deferred

Cross-backend comparison is provided where relevant. The Kotlin backend (JNA-based, JVM family) and the Dart backend (garbage-collected, similar lifecycle model) are the closest analogs.

---

### DONE

- [x] **`method_overloading`** — Java fully supports compile-time method overloading (same method name, different parameter lists). Already set to `true`. This is also useful for generating overloads that simulate default parameter values (see `default_args` below).

- [x] **`utf8_strings`** — The Java backend converts `String` parameters to UTF-8 bytes via `getBytes(StandardCharsets.UTF_8)` and passes them to native code with `Arena.allocateFrom()`.

- [x] **`utf16_strings`** — Java `String` parameters are converted to UTF-16 char arrays via `toCharArray()` and passed with `Arena.allocateFrom(ValueLayout.JAVA_CHAR, chars)`.

- [x] **`non_exhaustive_structs`** — Java classes are inherently non-exhaustive — adding fields doesn't break binary compatibility. Structs are generated as regular classes (not records).

- [x] **`option`** — `Option<T>` return types are wrapped in `Optional<T>` with boxed primitives. Optional opaque parameters pass `MemorySegment.NULL` when absent.

- [x] **`custom_errors`** — Struct and opaque error types extend `RuntimeException` and are thrown directly from fallible methods. Enum errors are thrown as `RuntimeException` with the error value in the message (pending proper Java enum class generation).

- [x] **`constructors`** — `#[diplomat::attr(auto, constructor)]` generates idiomatic `new TypeName(...)` Java constructors. Opaque constructors assign `this.handle`, struct constructors assign fields from native memory. Constructor syntax is not applied to enum types.

- [x] **`named_constructors`** — `#[diplomat::attr(auto, named_constructor = "name")]` generates named static factory methods (e.g., `ResultOpaque.failingFoo()`). Names are converted to lowerCamelCase and keyword-censored.

- [x] **`fallible_constructors`** — Constructors returning `Result<Box<Self>, E>` generate Java constructors that throw on error. The ok-branch assigns `this.handle` (opaque) or fields (struct); the error branch throws the appropriate exception type.

---

### TODO

- [ ] **`stringifiers`** — Map the `#[diplomat::attr(*, stringifier)]` method to a `toString()` override. Every Java class inherits `Object.toString()`, and overriding it is deeply idiomatic — it's automatically called by string concatenation (`"Value: " + obj`), `System.out.println()`, `String.format()`, and logging frameworks. The Kotlin backend already implements this as `override fun toString(): String`. The Dart backend maps it to `toString()` as well. The backend already supports `DiplomatWrite`-based returns — this flag just needs special-case naming.

- [ ] **`comparators`** — Map the `comparison` attribute to `Comparable<T>` implementation with a `compareTo()` method. Java's `Comparable<T>` interface is a standard part of the language, enabling natural use with `Collections.sort()`, `TreeMap`, `TreeSet`, and `Arrays.sort()`. Dart and C++ already support this. The Kotlin backend does *not* yet enable this flag, but Java's strongly-typed `Comparable<T>` makes it a clean fit. The Diplomat `comparison` method returns `std::cmp::Ordering`, which maps directly to Java's `compareTo()` contract (negative/zero/positive int).

- [x] **`iterators`** — Map the `iterator` attribute to Java's `Iterator<T>` interface (`hasNext()` + `next()`). Generates a private `nextInternal()` wrapper that returns raw nullable values, with one-ahead buffering to implement `hasNext()`. The `NoSuchElementException` is thrown when `next()` is called past the end. Supported by Kotlin, Dart, JS, C++, and nanobind.

- [x] **`iterables`** — Map the `iterable` attribute to Java's `Iterable<T>` interface, enabling enhanced for-each loop syntax (`for (Item item : collection) { ... }`). Generates an `@Override iterator()` method returning the concrete iterator class. The iterable item type is resolved from the iterator's `special_method_presence`. Supported by Kotlin, Dart, JS, C++, and nanobind.

- [x] **`indexing`** — Generate a `get(indexType)` method for the `indexer` attribute. Generates a private `getInternal()` wrapper that returns raw nullable values, with a public `get()` wrapper that throws `IndexOutOfBoundsException` for null results. Java does not support `[]` operator overloading, but `get()` is the idiomatic convention (`java.util.List`, etc.). Supported by Kotlin, Dart, C++, and nanobind.

- [x] **`callbacks`** — Callback function parameters (`impl Fn(args) -> ret`) generate `@FunctionalInterface` inner interfaces, static runner methods, and FFM `Linker.upcallStub()` stubs. At each call site, the Java lambda is registered in a `ConcurrentHashMap<Long, Object>` registry in `DiplomatLib.java`; a `DiplomatCallback` native struct is allocated with `{data=id, run_callback=stub, destructor=destructor_stub}`. Callbacks with str/opaque/slice args, result/option returns, and various conversion callbacks are disabled for Java via `#[diplomat::attr(java, disable)]`. Kotlin, C++, C, and nanobind also support this.

- [x] **`traits`** — Diplomat trait definitions generate Java `interface` files. Each interface includes the user-facing method signatures, a `VTABLE_LAYOUT` and `TRAIT_STRUCT_LAYOUT` for the native representation, a `Statics` inner class with per-method runner methods and upcall stubs, and a `createNative(Object impl_, Arena arena)` factory method. Trait methods returning `Result` are disabled for Java. Kotlin and C also support traits.

- [ ] **`owned_slices`** — Support for Rust-allocated slices that transfer ownership to the foreign side. The FFM API can manage these via `MemorySegment` with custom cleanup actions (using `Arena` or manual `MemorySegment.ofAddress()` with deallocation). The Kotlin backend wraps these in an `OwnedSlice` class with a raw pointer and length. Kotlin, Dart, and JS support this. Requires implementing basic slice support first.

- [ ] **`defaults`** — Support for the `default` attribute on types and enum variants, indicating a type has a default/zero state. C++ and nanobind support this. Once enums are implemented as proper Java enum classes, a default variant can be marked (e.g., via a `static` field or documentation convention). Low priority but straightforward.

- [ ] **`accessors`** — Map getter/setter attributes to methods. Java has no language-level property syntax, but the JavaBeans convention (`getXxx()` / `setXxx()`) is universally recognized by frameworks, tools, and IDEs. When a method is marked `#[diplomat::attr(*, getter = "a")]`, generate `public Type getA()` instead of the raw method name. Dart, JS, and nanobind support this. The Kotlin backend does *not* yet. JavaBeans-style accessors are arguably more useful in Java than in languages with native property syntax, since they integrate with reflection-based frameworks (Spring, Jackson, JPA).

- [ ] **`generate_mocking_interface`** — Generate an interface extracted from an opaque type's methods, allowing test mocking. Only Kotlin supports this today. In Java, this is extremely natural and useful — Java's testing ecosystem (Mockito, EasyMock) relies heavily on interfaces for mocking. The implementation mirrors Kotlin's: generate an internal `interface FooInterface` with all instance methods, then have the opaque class `implements FooInterface`. This allows test code to mock the interface without needing a native library.

- [ ] **`free_functions`** — Support generating functions not associated with any type. Java requires all functions to live inside a class, but the idiomatic pattern is a utility class with static methods (e.g., `Collections.sort()`, `Arrays.asList()`). The generated `DiplomatLib` class or a dedicated utility class can host these. C, C++, and nanobind support this.

- [ ] **`static_slices`** — Support `&'static` slice parameters. These are slices with the `'static` lifetime, meaning the data lives for the entire program. In Java, this can be handled by allocating a `MemorySegment` in the global arena (`Arena.global()`), which is never freed. Kotlin, C, C++, and nanobind support this. Requires basic slice support first.

- [ ] **`default_args`** — Support default parameter values via `#[diplomat::attr(*, default_value = ...)]`. Java does not have language-level default parameters, but method overloading is the standard workaround: generate an overload without the optional parameter that delegates to the full version with the default filled in. Since `method_overloading` is already `true`, this is the right approach. Only C++ currently supports this flag. The implementation involves generating additional method overloads for each suffix of optional parameters.

---

### WON'T DO

- [ ] **`memory_sharing`** — Indicates that Rust can directly access the foreign language's memory (e.g., reading a C struct by pointer). This requires the foreign language to have a predictable, stable memory layout that Rust can dereference — only possible in languages like C and C++ where the programmer controls memory layout directly. Java's memory is managed by the JVM garbage collector, which can relocate objects at any time (compacting GC), making direct pointer access from Rust unsafe and unreliable. C, C++, and nanobind (which compiles to a C extension) set this to `true`; no garbage-collected language backend (Kotlin, Dart, JS) enables it. The FFM API provides controlled memory sharing through `MemorySegment`, but that's Diplomat's struct/slice mechanism, not `memory_sharing`.

- [ ] **`namespacing`** — Apply C++-style `namespace` grouping to types. Java already has its own namespace system (packages), and the Java backend uses the `domain` config (e.g., `dev.diplomattest.somelib`) to place all generated classes in a Java package. Diplomat's `namespace` attribute is designed for languages like C++ where namespace nesting within a library is needed. In Java, all types within a library naturally live in the same package (or sub-packages), and the Diplomat `namespace` attribute would conflict with Java's package system. Only C++ and nanobind support this. The Kotlin backend also does not enable it.

- [ ] **`arithmetic`** — Overload `+`, `-`, `*`, `/` and their in-place variants (`+=`, `-=`, `*=`, `/=`). Java does **not** support operator overloading — this is a deliberate language design choice dating back to Java's creation, intended to prevent the readability issues seen in C++. The only overloaded operator in Java is `+` for `String` concatenation, and it is not user-extensible. While there are experimental Valhalla/value-type proposals that might eventually enable limited operator overloading, these have no JEP and are far from inclusion. Only C++ and nanobind support this flag. The Java backend should instead generate named methods (`add()`, `subtract()`, etc.) when these attributes are encountered, which is already what happens when the attribute is not recognized — the method falls through to normal method generation.

- [ ] **`static_accessors`** — Mark *static* methods (no `self` parameter) as getters/setters. This is a niche feature only supported by nanobind. Java has no static property syntax. Static getter/setter methods can be generated with `getXxx()` / `setXxx()` naming, but there's no language-level distinction from regular static methods, so the attribute provides no additional value over normal method generation. Not worth the implementation complexity for zero ergonomic benefit.

- [ ] **`traits_are_send`** — Indicate that trait objects are safe to send between threads (`std::marker::Send`). This is a Rust concurrency safety concept with no direct analog in Java. Java threads can share objects freely (all objects are `Send` in Rust terms by default in Java's model, with synchronization handled by `synchronized`, `volatile`, `java.util.concurrent`, etc.). Only Kotlin supports this flag, and it's used for JNA-specific thread safety guarantees. The FFM API handles thread safety differently (arenas can be confined or shared). Not applicable to the Java/FFM model.

- [ ] **`traits_are_sync`** — Indicate that trait objects are safe to share between threads (`std::marker::Sync`). Same reasoning as `traits_are_send`. Java's concurrency model does not map to Rust's `Send`/`Sync` marker traits. Only Kotlin supports this. Not applicable.

---

### MIGHT DO

- [ ] **`abi_compatibles`** — Allow structs marked `#[diplomat::attr(auto, abi_compatible)]` to be passed as slices and borrowed by pointer. This requires the Java-side struct representation to have an **identical memory layout** to the C ABI struct (matching field order, sizes, alignment, and padding). With the FFM API, this is technically possible using `StructLayout` + `MemorySegment` — the Java side would define a `StructLayout` matching the C layout and allocate/populate `MemorySegment` values accordingly. However, maintaining layout compatibility is fragile and error-prone: any mismatch between the generated Java `StructLayout` and the C struct layout causes silent memory corruption. C, C++, and nanobind support this (they share memory layout by definition). The decision depends on whether the Java backend generates struct layouts that provably match the C ABI — this may be achievable by deriving the layout from the same HIR data that the C backend uses, but it adds significant complexity and testing burden. **What would help:** Confirming that Diplomat's HIR provides enough information to deterministically compute C ABI struct layouts (including padding/alignment), and whether the FFM `StructLayout` API can faithfully reproduce them.

- [ ] **`struct_refs`** — Support `&Struct` and `&mut Struct` as function parameters (borrowing a struct by reference rather than by value). This requires passing a pointer to the struct's memory to the native function. With FFM, this means passing a `MemorySegment` pointing to the struct's data. Only C, C++, and nanobind support this — all languages where structs have predictable memory layout. The feasibility in Java depends on the same `StructLayout`/ABI-compatibility question as `abi_compatibles`. If the backend generates correct `StructLayout` definitions, struct references become a `MemorySegment` pointer pass. **What would help:** Same as `abi_compatibles` — confirming ABI layout correctness.

- [ ] **`custom_bindings`** — Allow users to inject custom code into generated classes via `#[diplomat::attr(java, custom_extra_code(...))]`. Only C++ and nanobind support this. The mechanism is straightforward (insert verbatim Java code at designated points in the template), but it raises maintenance concerns: custom code can break when the generated class structure changes, and it couples user code to internal codegen details. **What would help:** User demand. If Java users need escape hatches for functionality that Diplomat can't generate, this is a reasonable safety valve. Low priority until the backend is mature enough to have users hitting its limitations.
