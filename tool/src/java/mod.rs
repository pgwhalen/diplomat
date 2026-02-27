use askama::Template;
use diplomat_core::hir::{
    self, BackendAttrSupport, DocsUrlGenerator, Method, OutType, ReturnType, SelfType, Slice,
    SuccessType, Type, TypeContext, TypeDef,
};
use std::borrow::Cow;

mod formatter;
use formatter::JavaFormatter;

use crate::{Config, ErrorStore, FileMap};
use serde::{Deserialize, Serialize};

pub(crate) fn attr_support() -> BackendAttrSupport {
    let mut a = BackendAttrSupport::default();

    a.namespacing = false;
    a.memory_sharing = false;
    a.non_exhaustive_structs = false;
    a.method_overloading = true;
    a.utf8_strings = false;
    a.utf16_strings = false;
    a.static_slices = false;
    a.option = false;

    a.constructors = false;
    a.named_constructors = false;
    a.fallible_constructors = false;
    a.accessors = false;
    a.static_accessors = false;
    a.stringifiers = false;
    a.comparators = false;
    a.iterators = false;
    a.iterables = false;
    a.indexing = false;
    a.callbacks = false;
    a.traits = false;
    a.custom_errors = false;
    a.traits_are_send = false;
    a.traits_are_sync = false;
    a.generate_mocking_interface = false;
    a.owned_slices = false;

    a
}

#[derive(Debug, Clone, Deserialize, Serialize, Default)]
pub struct JavaConfig {
    domain: Option<String>,
    dylib_name: Option<String>,
}

impl JavaConfig {
    pub fn set(&mut self, key: &str, value: toml::Value) {
        match key {
            "domain" => {
                if value.is_str() {
                    self.domain = value.as_str().map(|s| s.to_string());
                }
            }
            "dylib_name" => {
                self.dylib_name = value.as_str().map(|val| val.to_string());
            }
            _ => {}
        }
    }
}

pub(crate) fn run<'tcx>(
    tcx: &'tcx TypeContext,
    conf: Config,
    docs_url_gen: &'tcx DocsUrlGenerator,
) -> (FileMap, ErrorStore<'tcx, String>) {
    let JavaConfig { domain, dylib_name } = conf.java_config;

    let domain = domain.expect("Failed to parse Java config. Missing required field `domain`.");

    let lib_name = conf
        .shared_config
        .lib_name
        .expect("Failed to parse Java config. Missing required field `lib_name`.");

    let dylib_name = dylib_name.as_deref().unwrap_or(&lib_name);

    let formatter = JavaFormatter::new(tcx, docs_url_gen);

    let files = FileMap::default();
    let errors = ErrorStore::default();

    let ty_gen_cx = ItemGenContext {
        tcx,
        errors: &errors,
        formatter: &formatter,
        lib_name: &lib_name,
        dylib_name,
        domain: &domain,
    };

    for (id, ty) in tcx.all_types() {
        let _guard = ty_gen_cx.errors.set_context_ty(ty.name().as_str().into());
        if ty.attrs().disable {
            continue;
        }
        match ty {
            TypeDef::Opaque(o) => {
                let type_name = formatter.fmt_type_name(id);
                let (file_name, body) = ty_gen_cx.gen_opaque_def(o, &type_name);
                files.add_file(file_name, body);
            }
            _ => {
                // Skip unsupported type kinds (structs, enums, etc.)
            }
        }
    }

    // Generate Lib.java runtime support file
    #[derive(Template)]
    #[template(path = "java/Lib.java.jinja", escape = "none")]
    struct LibTemplate<'a> {
        domain: &'a str,
        lib_name: &'a str,
    }

    let lib_body = LibTemplate {
        domain: &domain,
        lib_name: &lib_name,
    }
    .render()
    .expect("Failed to render Lib.java");

    files.add_file(
        format!(
            "src/main/java/{}/{lib_name}/DiplomatLib.java",
            domain.replace('.', "/")
        ),
        lib_body,
    );

    (files, errors)
}

struct ItemGenContext<'a, 'cx> {
    #[allow(dead_code)]
    tcx: &'cx TypeContext,
    lib_name: &'a str,
    dylib_name: &'a str,
    domain: &'a str,
    formatter: &'a JavaFormatter<'cx>,
    errors: &'a ErrorStore<'cx, String>,
}

struct JavaMethodInfo {
    definition: String,
}

struct JavaNativeMethodInfo {
    handle_name: String,
    abi_name: String,
    descriptor: String,
}

impl<'cx> ItemGenContext<'_, 'cx> {
    /// Check if a method uses only supported types
    fn is_method_supported(&self, method: &Method) -> bool {
        // Check return type
        match &method.output {
            ReturnType::Infallible(success) => match success {
                SuccessType::Unit => {}
                SuccessType::OutType(ty) => {
                    if !self.is_out_type_supported(ty) {
                        return false;
                    }
                }
                _ => return false,
            },
            _ => return false, // Fallible/Nullable not supported
        }

        // Check params
        for param in method.params.iter() {
            if !self.is_param_type_supported(&param.ty) {
                return false;
            }
        }

        true
    }

    fn is_param_type_supported<P: hir::TyPosition>(&self, ty: &Type<P>) -> bool {
        matches!(
            ty,
            Type::Primitive(_) | Type::Opaque(_) | Type::Slice(Slice::Str(_, _)) | Type::Enum(_)
        )
    }

    fn is_out_type_supported(&self, ty: &OutType) -> bool {
        matches!(ty, Type::Primitive(_) | Type::Opaque(_) | Type::Enum(_))
    }

    fn gen_opaque_def(&self, ty: &'cx hir::OpaqueDef, type_name: &str) -> (String, String) {
        let supported_methods: Vec<&Method> = ty
            .methods
            .iter()
            .filter(|m| !m.attrs.disable && self.is_method_supported(m))
            .collect();

        let native_methods: Vec<JavaNativeMethodInfo> = supported_methods
            .iter()
            .map(|method| self.gen_native_method_info(method))
            .collect();

        let companion_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter(|method| method.param_self.is_none())
            .map(|method| self.gen_method(method, None, type_name))
            .collect();

        let self_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter_map(|method| {
                method
                    .param_self
                    .as_ref()
                    .map(|self_param| (*method, &self_param.ty))
            })
            .map(|(method, self_type)| self.gen_method(method, Some(self_type), type_name))
            .collect();

        #[derive(Template)]
        #[template(path = "java/Opaque.java.jinja", escape = "none")]
        struct ImplTemplate<'a> {
            domain: &'a str,
            lib_name: &'a str,
            dylib_name: &'a str,
            type_name: &'a str,
            dtor_abi_name: &'a str,
            native_methods: &'a [JavaNativeMethodInfo],
            companion_methods: &'a [JavaMethodInfo],
            self_methods: &'a [JavaMethodInfo],
        }

        (
            format!(
                "src/main/java/{}/{lib_name}/{type_name}.java",
                self.domain.replace('.', "/"),
                lib_name = self.lib_name,
            ),
            ImplTemplate {
                domain: self.domain,
                lib_name: self.lib_name,
                dylib_name: self.dylib_name,
                type_name,
                dtor_abi_name: ty.dtor_abi_name.as_str(),
                native_methods: &native_methods,
                companion_methods: &companion_methods,
                self_methods: &self_methods,
            }
            .render()
            .expect("failed to render opaque type"),
        )
    }

    fn gen_native_method_info(&self, method: &'cx Method) -> JavaNativeMethodInfo {
        let handle_name = method.abi_name.as_str().to_uppercase();
        let abi_name = method.abi_name.as_str().to_string();

        let mut param_layouts = Vec::new();

        // Self parameter
        if method.param_self.is_some() {
            param_layouts.push("ValueLayout.ADDRESS".to_string());
        }

        // Regular parameters
        for param in method.params.iter() {
            self.push_param_layouts(&param.ty, &mut param_layouts);
        }

        // Build the descriptor
        let return_layout = self.get_return_layout(&method.output);
        let descriptor = if let Some(ret) = return_layout {
            if param_layouts.is_empty() {
                format!("FunctionDescriptor.of({ret})")
            } else {
                format!("FunctionDescriptor.of({ret}, {})", param_layouts.join(", "))
            }
        } else if param_layouts.is_empty() {
            "FunctionDescriptor.ofVoid()".to_string()
        } else {
            format!("FunctionDescriptor.ofVoid({})", param_layouts.join(", "))
        };

        JavaNativeMethodInfo {
            handle_name,
            abi_name,
            descriptor,
        }
    }

    fn push_param_layouts<P: hir::TyPosition>(&self, ty: &Type<P>, layouts: &mut Vec<String>) {
        match ty {
            Type::Primitive(prim) => {
                layouts.push(self.formatter.fmt_primitive_as_ffi(*prim).to_string());
            }
            Type::Opaque(_) => {
                layouts.push("ValueLayout.ADDRESS".to_string());
            }
            Type::Slice(Slice::Str(_, _)) => {
                // DiplomatStr is passed as { ADDRESS data, JAVA_LONG len }
                layouts.push("ValueLayout.ADDRESS".to_string());
                layouts.push("ValueLayout.JAVA_LONG".to_string());
            }
            Type::Enum(_) => {
                layouts.push("ValueLayout.JAVA_INT".to_string());
            }
            _ => {
                self.errors
                    .push_error(format!("Unsupported parameter type in Java backend: {ty:?}"));
            }
        }
    }

    fn get_return_layout(&self, output: &ReturnType) -> Option<String> {
        match output {
            ReturnType::Infallible(success) => self.get_success_layout(success),
            ReturnType::Fallible(_, _) | ReturnType::Nullable(_) => {
                self.errors.push_error(
                    "Fallible/Nullable return types not yet supported in Java backend".into(),
                );
                None
            }
        }
    }

    fn get_success_layout(&self, success: &SuccessType) -> Option<String> {
        match success {
            SuccessType::Unit => None,
            SuccessType::OutType(ty) => self.get_type_layout(ty),
            SuccessType::Write => {
                self.errors
                    .push_error("Write return type not yet supported in Java backend".into());
                None
            }
            _ => {
                self.errors
                    .push_error("Unsupported success type in Java backend".into());
                None
            }
        }
    }

    fn get_type_layout(&self, ty: &OutType) -> Option<String> {
        match ty {
            Type::Primitive(prim) => Some(self.formatter.fmt_primitive_as_ffi(*prim).to_string()),
            Type::Opaque(_) => Some("ValueLayout.ADDRESS".to_string()),
            Type::Enum(_) => Some("ValueLayout.JAVA_INT".to_string()),
            _ => {
                self.errors
                    .push_error(format!("Unsupported return type in Java backend: {ty:?}"));
                None
            }
        }
    }

    fn gen_method(
        &self,
        method: &'cx Method,
        self_type: Option<&SelfType>,
        owner_type_name: &str,
    ) -> JavaMethodInfo {
        let _guard = self.errors.set_context_method(method.name.as_str().into());

        let method_name = self.formatter.fmt_method_name(method);
        let abi_handle = method.abi_name.as_str().to_uppercase();

        let return_type_java = self.gen_return_type_java(&method.output, owner_type_name);
        let is_static = self_type.is_none();

        // Build parameter list for Java signature
        let mut java_params = Vec::new();
        // Build invoke arguments (what gets passed to invokeExact)
        let mut invoke_args = Vec::new();
        // Track string params that need Arena allocation
        let mut string_params: Vec<String> = Vec::new();

        if self_type.is_some() {
            invoke_args.push("handle".to_string());
        }

        for param in method.params.iter() {
            let param_name = self.formatter.fmt_param_name(param.name.as_str());
            self.gen_java_param(
                &param.ty,
                &param_name,
                &mut java_params,
                &mut invoke_args,
                &mut string_params,
            );
        }

        let java_params_str = java_params.join(", ");
        let args_str = invoke_args.join(", ");
        let has_string_param = !string_params.is_empty();

        // Build invoke expression
        let invoke_call = format!("{abi_handle}.invokeExact({args_str})");
        let return_stmt = match &method.output {
            ReturnType::Infallible(SuccessType::Unit) => format!("{invoke_call};"),
            ReturnType::Infallible(SuccessType::OutType(ty)) => {
                let wrapped = self.wrap_invoke_result(ty, &invoke_call);
                format!("return {wrapped};")
            }
            _ => "throw new UnsupportedOperationException();".to_string(),
        };

        // Build method body
        let body = if has_string_param {
            let mut setup_lines = String::new();
            for sp in &string_params {
                setup_lines.push_str(&format!(
                    "            byte[] {sp}Bytes = {sp}.getBytes(StandardCharsets.UTF_8);\n\
                     \n            var {sp}Seg = arena.allocateFrom(ValueLayout.JAVA_BYTE, {sp}Bytes);\n"
                ));
            }
            format!("        try (var arena = Arena.ofConfined()) {{\n{setup_lines}            {return_stmt}\n        }} catch (Throwable ex) {{\n            throw new RuntimeException(ex);\n        }}")
        } else {
            format!("        try {{\n            {return_stmt}\n        }} catch (Throwable ex) {{\n            throw new RuntimeException(ex);\n        }}")
        };

        let static_kw = if is_static { "static " } else { "" };

        let definition = format!(
            "public {static_kw}{return_type_java} {method_name}({java_params_str}) {{\n{body}\n    }}"
        );

        JavaMethodInfo { definition }
    }

    fn gen_java_param<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        param_name: &str,
        java_params: &mut Vec<String>,
        invoke_args: &mut Vec<String>,
        string_params: &mut Vec<String>,
    ) {
        match ty {
            Type::Primitive(prim) => {
                let java_type = self.formatter.fmt_primitive_as_java(*prim);
                java_params.push(format!("{java_type} {param_name}"));
                invoke_args.push(param_name.to_string());
            }
            Type::Opaque(_) => {
                let type_name: Cow<str> = ty
                    .id()
                    .map(|id| self.formatter.fmt_type_name(id))
                    .unwrap_or("MemorySegment".into());
                java_params.push(format!("{type_name} {param_name}"));
                invoke_args.push(format!("{param_name}.handle"));
            }
            Type::Slice(Slice::Str(_, _)) => {
                // DiplomatStr → Java String param, passed as (data_ptr, len) to native
                java_params.push(format!("String {param_name}"));
                string_params.push(param_name.to_string());
                invoke_args.push(format!("{param_name}Seg"));
                invoke_args.push(format!("(long) {param_name}Bytes.length"));
            }
            Type::Enum(_) => {
                java_params.push(format!("int {param_name}"));
                invoke_args.push(param_name.to_string());
            }
            _ => {
                self.errors
                    .push_error(format!("Unsupported parameter type in Java: {ty:?}"));
                java_params.push(format!("Object {param_name}"));
                invoke_args.push(param_name.to_string());
            }
        }
    }

    fn gen_return_type_java(&self, output: &ReturnType, owner_type_name: &str) -> String {
        match output {
            ReturnType::Infallible(success) => match success {
                SuccessType::Unit => "void".to_string(),
                SuccessType::OutType(ty) => self.gen_out_type_java(ty, owner_type_name),
                SuccessType::Write => "String".to_string(),
                _ => {
                    self.errors
                        .push_error("Unsupported return type in Java backend".into());
                    "Object".to_string()
                }
            },
            _ => {
                self.errors.push_error(
                    "Fallible/Nullable return types not yet supported in Java backend".into(),
                );
                "Object".to_string()
            }
        }
    }

    fn gen_out_type_java(&self, ty: &OutType, owner_type_name: &str) -> String {
        match ty {
            Type::Primitive(prim) => self.formatter.fmt_primitive_as_java(*prim).to_string(),
            Type::Opaque(op) => {
                let type_id = ty.id().expect("opaque must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                if op.is_optional() {
                    type_name.to_string()
                } else {
                    type_name.to_string()
                }
            }
            Type::Enum(_) => "int".to_string(),
            _ => {
                self.errors.push_error(format!(
                    "Unsupported return type in Java backend for {owner_type_name}: {ty:?}"
                ));
                "Object".to_string()
            }
        }
    }

    /// Wrap the raw invokeExact call with appropriate casting/construction
    fn wrap_invoke_result(&self, ty: &OutType, invoke_call: &str) -> String {
        match ty {
            Type::Primitive(prim) => {
                let cast = self.formatter.fmt_primitive_as_java(*prim);
                format!("({cast}) {invoke_call}")
            }
            Type::Opaque(_) => {
                let type_id = ty.id().expect("opaque must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!("new {type_name}((MemorySegment) {invoke_call})")
            }
            Type::Enum(_) => format!("(int) {invoke_call}"),
            _ => invoke_call.to_string(),
        }
    }


}

#[cfg(test)]
mod test {
    use diplomat_core::hir::TypeDef;
    use quote::quote;

    use crate::ErrorStore;

    use super::formatter::test::new_tcx;
    use super::{formatter::JavaFormatter, ItemGenContext};

    fn gen_opaque_for_test(tk_stream: proc_macro2::TokenStream) -> String {
        let tcx = new_tcx(tk_stream);
        let docs_urls = std::collections::HashMap::new();
        let docs_generator = &diplomat_core::hir::DocsUrlGenerator::with_base_urls(None, docs_urls);
        let formatter = JavaFormatter::new(&tcx, docs_generator);
        let errors = ErrorStore::default();

        let cx = ItemGenContext {
            tcx: &tcx,
            formatter: &formatter,
            errors: &errors,
            lib_name: "somelib",
            dylib_name: "diplomat_example",
            domain: "dev.diplomattest",
        };

        let mut result = String::new();
        for (_id, ty) in tcx.all_types() {
            if let TypeDef::Opaque(o) = ty {
                let (_file, body) = cx.gen_opaque_def(o, &o.name.to_string());
                result.push_str(&body);
            }
        }
        result
    }

    #[test]
    fn test_opaque_simple() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            #[diplomat::abi_rename = "icu4x_{0}_mv1"]
            mod ffi {
                #[diplomat::opaque]
                struct Locale(());

                impl Locale {
                    pub fn new(name: &DiplomatStr) -> Box<Locale> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_opaque_with_primitives() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct Counter(());

                impl Counter {
                    pub fn new(start: i32) -> Box<Counter> {
                        unimplemented!()
                    }

                    pub fn get_value(&self) -> i32 {
                        unimplemented!()
                    }

                    pub fn add(&self, amount: i32) {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }
}
