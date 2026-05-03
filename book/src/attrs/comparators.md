# Comparators

({{get_supports("comparators")}}. Intended to be supported by `kotlin`)

Some languages allow for overloading the `<`/`>`/`=`/etc operators, similar to Rust's `PartialOrd`.

To expose this over Diplomat, use the `comparison` attribute on a method that takes another `Self` parameter:

```rust
#[diplomat::bridge]
mod ffi {
    #[diplomat::opaque]
    #[derive(Ord, PartialOrd, Eq, PartialEq)]
    struct Foo(u8);

    impl Foo {
        #[diplomat::attr(auto, comparison)]
        pub fn compare(&self, other: &Foo) -> std::cmp::Ordering {
            self.cmp(other)
        }
    }
}
```

In Dart, this will provide an overloaded `==`, and `Foo` will implement `Comparable<Foo>`.

We currently do not but Diplomat would like to also support the ability to _just_ overload `==`.


## Partial Comparisons
({{get_supports("partial_comparators")}})

Some backends support partial comparisons, which return `Option<std::cmp::Ordering>`:
```rust
#[diplomat::bridge]
mod ffi {
    #[diplomat::opaque]
    #[derive(Ord, PartialOrd, Eq, PartialEq)]
    struct Foo(u8);

    impl Foo {
        #[diplomat::attr(auto, comparison)]
        pub fn compare(&self, other: &Foo) -> Option<std::cmp::Ordering> {
            self.cmp(other)
        }
    }
}
```
This is intended for comparison of types that implement `PartialEq` only, like `f32`; since `NaN` is a valid value for `f32` and cannot be compared against other values (and so returns `None`).