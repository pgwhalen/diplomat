# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Diplomat is a Rust tool for generating FFI bindings that allow other languages (C, C++, JavaScript/TypeScript, Dart, Kotlin/JNA, Python/nanobind, Java) to call Rust code. Users define Rust APIs inside `#[diplomat::bridge]` modules, and Diplomat generates idiomatic high-level bindings.

## Build & Test Commands

### Rust (core workflow)
```bash
cargo build                    # Build all workspace crates
cargo test                     # Run Rust unit tests (includes insta snapshot tests)
cargo clippy --all-targets --all-features -- -D warnings   # Lint
cargo fmt --all -- --check     # Format check
```

### Snapshot tests (cargo-insta)
Diplomat uses `insta` for snapshot testing of macro expansion and code generation. When codegen changes:
```bash
cargo insta review             # Review and accept/reject snapshot changes
```

### Cross-language tests (cargo-make)
Requires `cargo install cargo-make`.

```bash
cargo make gen                 # Regenerate all bindings for example/ and feature_tests/
cargo make gen-feature         # Regenerate all bindings for feature_tests/ only
cargo make gen-example         # Regenerate all bindings for example/ only
cargo make gen-<backend>-feature   # e.g., gen-cpp-feature, gen-js-feature, gen-kotlin-feature
cargo make gen-<backend>-example   # e.g., gen-c-example, gen-dart-example

cargo make test-cpp            # C++ tests (example + feature_tests)
cargo make test-c              # C tests (example only)
cargo make test-js             # JS tests (requires wasm32-unknown-unknown target)
cargo make test-dart           # Dart tests
cargo make test-kotlin         # Kotlin tests (Gradle)
cargo make test-nanobind       # Python nanobind tests (requires uv)
cargo make test-java           # Java tests

cargo make check-c             # Verify C headers compile
cargo make check-cpp           # Verify C++ headers compile
cargo make check-whitespace    # Check for trailing whitespace in generated code
```

CI regenerates all bindings and verifies no diff, so generated output must be committed.

## Architecture

### Data Flow
1. User writes Rust with `#[diplomat::bridge]` modules (using `diplomat` proc-macro + `diplomat-runtime` types)
2. The proc-macro (`macro/`) expands bridge modules, generating `#[no_mangle] extern "C"` FFI functions. Uses `diplomat_core::ast` for parsing.
3. `diplomat-tool` (`tool/`) reads Rust source via `syn-inline-mod`, parses to AST, lowers to HIR (`diplomat_core::hir::TypeContext`), then runs a backend to emit bindings.

### Workspace Crates
| Crate | Path | Role |
|-------|------|------|
| `diplomat_core` | `core/` | AST (`core/src/ast/`) and HIR (`core/src/hir/`, behind `hir` feature). Shared between macro and tool. |
| `diplomat` | `macro/` | Proc-macro crate providing `#[diplomat::bridge]` |
| `diplomat-runtime` | `runtime/` | FFI runtime types (`DiplomatWrite`, `DiplomatSlice`, etc.). `no_std` compatible. |
| `diplomat-tool` | `tool/` | CLI + all backend implementations |

### Backend Structure
Each backend lives in `tool/src/<backend>/` with Askama templates in `tool/templates/<backend>/`. A backend typically has:
- `mod.rs` — `run()` entry point and `attr_support()` declaration
- `formatter.rs` — type name formatting for the target language
- `gen.rs` — generation logic (optional, some backends inline this in mod.rs)

Backends write generated files into a `FileMap` which is then written to disk.

The `gen()` function in `tool/src/lib.rs` dispatches to backends by name: `"c"`, `"cpp"`, `"dart"`, `"js"`, `"demo_gen"`, `"java"`, `"kotlin"`, `"py-nanobind"` / `"nanobind"`.

### Configuration System
Layered config (`tool/src/config.rs`): TOML file (default `config.toml`) < CLI `--config key=value` < in-source `#[diplomat::config(...)]` attrs. Per-backend overrides use dotted keys (e.g., `[kotlin]`).

### Test Suites
- `feature_tests/` — comprehensive tests for all Diplomat features, with generated bindings + test harnesses per backend
- `example/` — real-world example wrapping ICU4X APIs, also with per-backend generated output

### Adding a New Backend
Per `Makefile.toml` comments: create `tool/src/<backend>/`, add templates in `tool/templates/<backend>/`, register in `tool/src/lib.rs` `gen()` match, update `Makefile.toml` gen/test tasks, and add directories to `.gitattributes`.

## Java Backend

The Java backend is under active development. See [java-backend.md](java-backend.md) for implementation status, feature checklist, and architecture details.

## Key Conventions
- MSRV 1.81 for library crates (`diplomat`, `diplomat_core`, `diplomat-runtime`); no MSRV for `diplomat-tool`
- Generated code is checked into the repo and marked in `.gitattributes` as `linguist-generated=true`
- The JS backend targets WebAssembly with C spec ABI (see README for wasm32 ABI notes)
