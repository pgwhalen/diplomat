# Java Backend for Diplomat

This document tracks the effort to build a Java backend for Diplomat. When implementing any new feature, consult `book/src/developer.md` for the recommended approach to building and testing backend features incrementally.

## Approach

The Java backend uses the **Java FFM (Foreign Function & Memory) API** (`java.lang.foreign.*`) rather than JNI. This requires JDK 22+. Opaque types implement `AutoCloseable` for deterministic native memory cleanup.

## Key Files

- `tool/src/java/mod.rs` — backend entry point (`run()`, `attr_support()`, `ItemGenContext` codegen)
- `tool/src/java/formatter.rs` — Java name/type formatting, keyword avoidance
- `tool/templates/java/Opaque.java.jinja` — Askama template for opaque type classes
- `tool/templates/java/Lib.java.jinja` — `DiplomatLib.java` runtime support (string view layout)
- `example/java/somelib/` — Gradle-based example project (JDK 25, JUnit 5)
- `example/config.toml` — `[java]` section with `domain` and `dylib-name`
- `tool/src/java/snapshots/` — insta snapshot tests

## Commands

```bash
cargo make gen-java-example    # Regenerate Java bindings for example/
cargo make test-java-example   # Build native lib + run Gradle tests
cargo test -p diplomat-tool -- java::test   # Run Java backend unit/snapshot tests
```

## Current Status

### What works

- **Opaque types** with `AutoCloseable` / destroy via FFM downcall handles
- **Static factory methods** (constructors returning `Box<Self>`)
- **Instance methods** with `&self`
- **Primitive parameters and returns** (all integer sizes, float, double, boolean)
- **Opaque parameters and returns** (passed as `MemorySegment`)
- **`&DiplomatStr` parameters** (Java `String` converted to UTF-8 bytes via `Arena`)
- **Enum parameters and returns** (as raw `int` — no Java enum class generated)
- **Method name renaming** via `#[diplomat::attr]`
- **Java keyword collision avoidance**

### What doesn't work yet

- Structs — completely skipped in `run()` (only `TypeDef::Opaque` is handled)
- Enums as proper Java enum classes — currently passed as raw `int`
- Fallible return types (Result) — methods filtered out (e.g., `FixedDecimalFormatter` constructor)
- Nullable return types (Option) — filtered out
- Write return type (stringifiers) — filtered out
- Slices other than `&str` — not supported
- Callbacks / traits
- Iterators / iterables
- Named constructors / accessors / comparators / indexing
- UTF-16 strings
- Feature tests — no `feature_tests/java/` directory exists yet
- Java is not yet included in CI meta-tasks (`test-example`, `test-feature`, `test-all`)

### `attr_support()` flags

In `tool/src/java/mod.rs`, nearly everything is `false` except `method_overloading`. Flags should be flipped to `true` as features are implemented.

## Feature Checklist

Copied from `book/src/developer.md` — check off features as they are added to the Java backend:

- [x] **primitive types**: All integer sizes, float, double, boolean mapped to Java equivalents
- [ ] **opaque types**:
  - [x] basic definition
  - [x] return a boxed opaque (with `AutoCloseable` / destroy cleanup)
  - [x] as self parameter
  - [x] as another parameter
- [ ] **structs**
- [ ] **enums** (as proper Java enum classes, not raw `int`)
- [ ] **writeable** (DiplomatWrite / stringifiers)
- [ ] **slices**:
  - [ ] primitive slices
  - [x] str slices (`&DiplomatStr` mapped to Java `String`)
  - [ ] owned slices
  - [ ] slices of strings
  - [ ] strings
- [ ] **borrows** — ensure managed objects aren't cleaned up while something depends on them
  - [ ] borrows of parameters
  - [ ] in struct fields
- [ ] **nullables** — returning Option types
- [ ] **fallibles** — returning Result types (discriminated union)
