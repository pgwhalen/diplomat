use diplomat_core::hir::{
    self, Docs, DocsUrlGenerator, FloatType, IntSizeType, IntType, PrimitiveType, TraitId,
    TypeContext, TypeId,
};
use heck::{ToLowerCamelCase, ToShoutySnakeCase};
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

fn censor_symbol_for_keywords(name: Cow<'_, str>) -> Cow<'_, str> {
    if KEYWORDS.contains(&&*name) {
        format!("{name}_").into()
    } else {
        name
    }
}

impl<'tcx> JavaFormatter<'tcx> {
    pub fn new(tcx: &'tcx TypeContext, docs_url_gen: &'tcx DocsUrlGenerator) -> Self {
        Self { tcx, docs_url_gen }
    }

    pub fn tcx(&self) -> &'tcx TypeContext {
        self.tcx
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
            PrimitiveType::Bool => "ValueLayout.JAVA_BOOLEAN",
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
        censor_symbol_for_keywords(name)
    }

    pub fn fmt_named_constructor_name<'a>(&self, name: &Option<String>, method: &'a hir::Method) -> Cow<'a, str> {
        let raw = name.as_deref().unwrap_or(method.name.as_str());
        let camel = raw.to_lower_camel_case();
        let renamed = method.attrs.rename.apply(camel.into());
        censor_symbol_for_keywords(renamed)
    }

    pub fn fmt_param_name<'a>(&self, ident: &'a str) -> Cow<'tcx, str> {
        let name = ident.to_lower_camel_case();
        censor_symbol_for_keywords(name.into())
    }

    pub fn fmt_field_name<'a>(&self, ident: &'a str) -> Cow<'tcx, str> {
        self.fmt_param_name(ident)
    }

    pub fn fmt_primitive_as_java_boxed(&self, prim: PrimitiveType) -> &'static str {
        match prim {
            PrimitiveType::Bool => "Boolean",
            PrimitiveType::Char => "Integer",
            PrimitiveType::Int(IntType::I8) | PrimitiveType::Ordering => "Byte",
            PrimitiveType::Int(IntType::I16) => "Short",
            PrimitiveType::Int(IntType::I32) => "Integer",
            PrimitiveType::Int(IntType::I64) => "Long",
            PrimitiveType::Int(IntType::U8) => "Byte",
            PrimitiveType::Int(IntType::U16) => "Short",
            PrimitiveType::Int(IntType::U32) => "Integer",
            PrimitiveType::Int(IntType::U64) => "Long",
            PrimitiveType::Byte => "Byte",
            PrimitiveType::IntSize(IntSizeType::Isize) => "Long",
            PrimitiveType::IntSize(IntSizeType::Usize) => "Long",
            PrimitiveType::Float(FloatType::F32) => "Float",
            PrimitiveType::Float(FloatType::F64) => "Double",
            PrimitiveType::Int128(_) => panic!("i128 not supported in Java"),
        }
    }

    pub fn fmt_enum_variant_name<'a>(&self, variant: &'a hir::EnumVariant) -> Cow<'a, str> {
        let name = variant.name.as_str().to_shouty_snake_case();
        let name = variant.attrs.rename.apply(name.into());
        censor_symbol_for_keywords(name)
    }

    pub fn fmt_type_name(&self, id: TypeId) -> Cow<'tcx, str> {
        let resolved = self.tcx.resolve_type(id);
        let candidate: Cow<str> = resolved.name().as_str().into();
        resolved.attrs().rename.apply(candidate)
    }

    pub fn fmt_trait_name(&self, id: TraitId) -> Cow<'tcx, str> {
        let resolved = self.tcx.resolve_trait(id);
        let candidate: Cow<str> = resolved.name.as_str().into();
        resolved.attrs.rename.apply(candidate)
    }

    pub fn fmt_trait_method_name<'a>(&self, method: &'a hir::Callback) -> Cow<'a, str> {
        let name = method
            .name
            .as_ref()
            .expect("trait methods must have a name");
        let camel = name.as_str().to_lower_camel_case();
        censor_symbol_for_keywords(camel.into())
    }

    /// Returns (size, alignment) in bytes for a primitive type in the C ABI.
    pub fn primitive_size_align(&self, prim: PrimitiveType) -> (usize, usize) {
        match prim {
            PrimitiveType::Bool => (1, 1),
            PrimitiveType::Char => (4, 4), // DiplomatChar = u32
            PrimitiveType::Int(IntType::I8)
            | PrimitiveType::Int(IntType::U8)
            | PrimitiveType::Ordering
            | PrimitiveType::Byte => (1, 1),
            PrimitiveType::Int(IntType::I16) | PrimitiveType::Int(IntType::U16) => (2, 2),
            PrimitiveType::Int(IntType::I32) | PrimitiveType::Int(IntType::U32) => (4, 4),
            PrimitiveType::Int(IntType::I64) | PrimitiveType::Int(IntType::U64) => (8, 8),
            PrimitiveType::IntSize(IntSizeType::Isize | IntSizeType::Usize) => (8, 8),
            PrimitiveType::Float(FloatType::F32) => (4, 4),
            PrimitiveType::Float(FloatType::F64) => (8, 8),
            PrimitiveType::Int128(_) => panic!("i128 not supported in Java"),
        }
    }

    pub fn fmt_docs(&self, docs: &Docs) -> String {
        docs.to_markdown(hir::DocsTypeReferenceSyntax::AtLink, self.docs_url_gen)
            .trim()
            .replace(" \n", "\n")
            .to_string()
    }

    /// Format docs as a complete JavaDoc block string (e.g. "/**\n * line\n */")
    /// with the given indentation prefix. Returns empty string if no docs.
    pub fn fmt_javadoc_block(&self, docs: &Docs, indent: &str) -> String {
        let md = self.fmt_docs(docs);
        if md.is_empty() {
            return String::new();
        }
        let mut block = format!("{indent}/**\n");
        for line in md.lines() {
            let trimmed = line.trim_end();
            if trimmed.is_empty() {
                block.push_str(&format!("{indent} *\n"));
            } else {
                block.push_str(&format!("{indent} * {trimmed}\n"));
            }
        }
        block.push_str(&format!("{indent} */\n"));
        block
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
