use diplomat_core::hir::{
    self, DocsUrlGenerator, FloatType, IntSizeType, IntType, PrimitiveType, TypeContext, TypeId,
};
use heck::ToLowerCamelCase;
use std::borrow::Cow;
use std::collections::HashSet;
use std::sync::LazyLock;

pub(super) struct JavaFormatter<'tcx> {
    tcx: &'tcx TypeContext,
    docs_url_gen: &'tcx DocsUrlGenerator,
}

static KEYWORDS: LazyLock<HashSet<&str>> = LazyLock::new(|| {
    [
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "void",
        "volatile",
        "while",
    ]
    .iter()
    .copied()
    .collect()
});

impl<'tcx> JavaFormatter<'tcx> {
    pub fn new(tcx: &'tcx TypeContext, docs_url_gen: &'tcx DocsUrlGenerator) -> Self {
        Self { tcx, docs_url_gen }
    }

    pub fn fmt_primitive_as_java(&self, prim: PrimitiveType) -> &'static str {
        match prim {
            PrimitiveType::Bool => "boolean",
            PrimitiveType::Char => "int",
            PrimitiveType::Int(IntType::I8) | PrimitiveType::Ordering => "byte",
            PrimitiveType::Int(IntType::I16) => "short",
            PrimitiveType::Int(IntType::I32) => "int",
            PrimitiveType::Int(IntType::I64) => "long",
            PrimitiveType::Int(IntType::U8) => "byte",
            PrimitiveType::Int(IntType::U16) => "short",
            PrimitiveType::Int(IntType::U32) => "int",
            PrimitiveType::Int(IntType::U64) => "long",
            PrimitiveType::Byte => "byte",
            PrimitiveType::IntSize(IntSizeType::Isize) => "long",
            PrimitiveType::IntSize(IntSizeType::Usize) => "long",
            PrimitiveType::Float(FloatType::F32) => "float",
            PrimitiveType::Float(FloatType::F64) => "double",
            PrimitiveType::Int128(_) => panic!("i128 not supported in Java"),
        }
    }

    pub fn fmt_primitive_as_ffi(&self, prim: PrimitiveType) -> &'static str {
        match prim {
            PrimitiveType::Bool => "ValueLayout.JAVA_BYTE",
            PrimitiveType::Char => "ValueLayout.JAVA_INT",
            PrimitiveType::Int(IntType::I8)
            | PrimitiveType::Int(IntType::U8)
            | PrimitiveType::Ordering
            | PrimitiveType::Byte => "ValueLayout.JAVA_BYTE",
            PrimitiveType::Int(IntType::I16) | PrimitiveType::Int(IntType::U16) => {
                "ValueLayout.JAVA_SHORT"
            }
            PrimitiveType::Int(IntType::I32) | PrimitiveType::Int(IntType::U32) => {
                "ValueLayout.JAVA_INT"
            }
            PrimitiveType::Int(IntType::I64) | PrimitiveType::Int(IntType::U64) => {
                "ValueLayout.JAVA_LONG"
            }
            PrimitiveType::IntSize(IntSizeType::Isize | IntSizeType::Usize) => {
                "ValueLayout.JAVA_LONG"
            }
            PrimitiveType::Float(FloatType::F32) => "ValueLayout.JAVA_FLOAT",
            PrimitiveType::Float(FloatType::F64) => "ValueLayout.JAVA_DOUBLE",
            PrimitiveType::Int128(_) => panic!("i128 not supported in Java"),
        }
    }

    pub fn fmt_method_name<'a>(&self, method: &'a hir::Method) -> Cow<'a, str> {
        let name = method.name.as_str().to_lower_camel_case();
        let name = method.attrs.rename.apply(name.into());
        if KEYWORDS.contains(&&*name) {
            format!("{name}_").into()
        } else {
            name
        }
    }

    pub fn fmt_param_name<'a>(&self, ident: &'a str) -> Cow<'tcx, str> {
        let name = ident.to_lower_camel_case();
        if KEYWORDS.contains(&*name) {
            format!("{name}_").into()
        } else {
            name.into()
        }
    }

    pub fn fmt_type_name(&self, id: TypeId) -> Cow<'tcx, str> {
        let resolved = self.tcx.resolve_type(id);
        let candidate: Cow<str> = resolved.name().as_str().into();
        resolved.attrs().rename.apply(candidate)
    }

    #[allow(dead_code)]
    pub fn fmt_docs_url_gen(&self) -> &DocsUrlGenerator {
        self.docs_url_gen
    }
}

#[cfg(test)]
pub mod test {
    use super::*;
    use diplomat_core::hir;
    use proc_macro2::TokenStream;
    use quote::quote;

    pub fn new_tcx(tk_stream: TokenStream) -> TypeContext {
        let file = syn::parse2::<syn::File>(tk_stream).expect("failed to parse item");
        let mut attr_validator = hir::BasicAttributeValidator::new("java_test");
        attr_validator.support = super::super::attr_support();
        match TypeContext::from_syn(&file, Default::default(), attr_validator) {
            Ok(context) => context,
            Err(e) => {
                for (_cx, err) in e {
                    eprintln!("Lowering error: {err}");
                }
                panic!("Failed to create context")
            }
        }
    }

    #[test]
    fn test_type_name() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct MyOpaqueStruct {
                    a: SomeExternalType
                }

                impl MyOpaqueStruct {
                    pub fn new() -> Box<MyOpaqueStruct> {
                        unimplemented!();
                    }

                    pub fn get_byte(&self) -> u8 {
                        unimplemented!()
                    }
                }
            }
        };
        let tcx = new_tcx(tk_stream);
        let docs_urls = std::collections::HashMap::new();
        let docs_generator = &diplomat_core::hir::DocsUrlGenerator::with_base_urls(None, docs_urls);
        let formatter = JavaFormatter::new(&tcx, docs_generator);
        let mut all_types = tcx.all_types();
        let (ty_id, _) = all_types.next().expect("Failed to get next type");
        assert_eq!(Cow::from("MyOpaqueStruct"), formatter.fmt_type_name(ty_id));
    }
}
