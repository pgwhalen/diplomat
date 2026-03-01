use askama::Template;
use diplomat_core::hir::{
    self, BackendAttrSupport, DocsUrlGenerator, Method, OutType, ReturnType, SelfType, Slice,
    StringEncoding, StructField, SuccessType, Type, TypeContext, TypeDef, TypeId,
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
    a.non_exhaustive_structs = true;
    a.method_overloading = true;
    a.utf8_strings = true;
    a.utf16_strings = true;
    a.static_slices = false;
    a.option = true;

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
    a.custom_errors = true;
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
                let is_error = o.attrs.custom_errors;
                let (file_name, body) = ty_gen_cx.gen_opaque_def(o, &type_name, is_error);
                files.add_file(file_name, body);
            }
            TypeDef::Struct(s) => {
                let type_name = formatter.fmt_type_name(id);
                let (file_name, body) = ty_gen_cx.gen_struct_def(s, &type_name, false);
                files.add_file(file_name, body);
            }
            TypeDef::OutStruct(s) => {
                let type_name = formatter.fmt_type_name(id);
                let (file_name, body) = ty_gen_cx.gen_struct_def(s, &type_name, true);
                files.add_file(file_name, body);
            }
            TypeDef::Enum(e) => {
                let type_name = formatter.fmt_type_name(id);
                let is_error = e.attrs.custom_errors;
                let (file_name, body) = ty_gen_cx.gen_enum_def(e, &type_name, is_error);
                files.add_file(file_name, body);
            }
            _ => {
                // Skip unsupported type kinds
            }
        }
    }

    // Generate Lib.java runtime support file
    #[derive(Template)]
    #[template(path = "java/Lib.java.jinja", escape = "none")]
    struct LibTemplate<'a> {
        domain: &'a str,
        lib_name: &'a str,
        dylib_name: &'a str,
    }

    let lib_body = LibTemplate {
        domain: &domain,
        lib_name: &lib_name,
        dylib_name,
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
    result_layout: Option<String>,
    result_layout_name: Option<String>,
}

struct JavaStructFieldInfo {
    field_name: String,
    java_type: String,
    from_native_expr: String,
    to_native_stmt: String,
}

struct JavaEnumVariantInfo {
    variant_name: String,
    discriminant: isize,
}

/// Alignment helper: compute (size, alignment) for an OutType in the C ABI.
fn out_type_size_align(ty: &OutType, formatter: &JavaFormatter) -> (usize, usize) {
    field_size_align_generic(ty, formatter)
}

/// Compute (size, alignment) for a type by its TypeId (works for any position).
fn type_size_align_by_id(type_id: TypeId, formatter: &JavaFormatter) -> (usize, usize) {
    let resolved = formatter.tcx().resolve_type(type_id);
    match resolved {
        TypeDef::Struct(s) => {
            compute_struct_fields_size_align(
                s.fields.iter().map(|f| field_size_align_generic(&f.ty, formatter)),
            )
        }
        TypeDef::OutStruct(s) => {
            compute_struct_fields_size_align(
                s.fields.iter().map(|f| field_size_align_generic(&f.ty, formatter)),
            )
        }
        TypeDef::Opaque(_) => (8, 8), // pointer
        TypeDef::Enum(_) => (4, 4),   // i32
        _ => (0, 0),
    }
}

/// Alias for backward compat
fn struct_size_align_by_id(type_id: TypeId, formatter: &JavaFormatter) -> (usize, usize) {
    type_size_align_by_id(type_id, formatter)
}

/// Get (size, align) for any Type<P>.
fn field_size_align_generic<P: hir::TyPosition>(ty: &Type<P>, formatter: &JavaFormatter) -> (usize, usize) {
    match ty {
        Type::Primitive(prim) => formatter.primitive_size_align(*prim),
        Type::Opaque(_) => (8, 8),
        Type::Enum(_) => (4, 4),
        Type::Struct(_) => {
            let type_id = ty.id().expect("struct must have id");
            struct_size_align_by_id(type_id, formatter)
        }
        Type::Slice(_) => (16, 8), // { pointer data, size_t len }
        Type::DiplomatOption(inner) => {
            let (inner_size, inner_align) = field_size_align_generic(inner, formatter);
            if inner_align == 0 {
                return (0, 0);
            }
            // DiplomatOption<T> = { T value; bool is_ok; } + trailing padding
            let total = align_up(inner_size + 1, inner_align);
            (total, inner_align)
        }
        _ => (0, 0),
    }
}

fn align_up(size: usize, align: usize) -> usize {
    if align == 0 {
        return size;
    }
    (size + align - 1) & !(align - 1)
}

/// Compute overall (size, alignment) from an iterator of field (size, align) pairs.
fn compute_struct_fields_size_align(
    fields: impl Iterator<Item = (usize, usize)>,
) -> (usize, usize) {
    let mut offset: usize = 0;
    let mut max_align: usize = 1;
    for (f_size, f_align) in fields {
        if f_align > 0 {
            let padding = (f_align - (offset % f_align)) % f_align;
            offset += padding;
        }
        offset += f_size;
        if f_align > max_align {
            max_align = f_align;
        }
    }
    // Trailing padding
    if max_align > 0 {
        let trailing = (max_align - (offset % max_align)) % max_align;
        offset += trailing;
    }
    (offset, max_align)
}

impl<'cx> ItemGenContext<'_, 'cx> {
    /// Check if a method uses only supported types
    fn is_method_supported(&self, method: &Method) -> bool {
        // Check return type
        let success_supported = |success: &SuccessType| -> bool {
            match success {
                SuccessType::Unit | SuccessType::Write => true,
                SuccessType::OutType(ty) => self.is_out_type_supported(ty),
                _ => false,
            }
        };
        match &method.output {
            ReturnType::Infallible(success) => {
                if !success_supported(success) {
                    return false;
                }
            }
            ReturnType::Fallible(ok, err) => {
                if !success_supported(ok) {
                    return false;
                }
                if let Some(err_ty) = err {
                    if !self.is_out_type_supported(err_ty) {
                        return false;
                    }
                }
            }
            ReturnType::Nullable(ok) => {
                if !success_supported(ok) {
                    return false;
                }
            }
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
            Type::Primitive(_)
                | Type::Opaque(_)
                | Type::Slice(Slice::Str(_, _))
                | Type::Enum(_)
                | Type::Struct(_)
        )
    }

    fn is_out_type_supported(&self, ty: &OutType) -> bool {
        matches!(
            ty,
            Type::Primitive(_) | Type::Opaque(_) | Type::Enum(_) | Type::Struct(_)
        )
    }

    fn gen_opaque_def(
        &self,
        ty: &'cx hir::OpaqueDef,
        type_name: &str,
        is_error: bool,
    ) -> (String, String) {
        let supported_methods: Vec<&Method> = ty
            .methods
            .iter()
            .filter(|m| !m.attrs.disable && self.is_method_supported(m))
            .collect();

        let uses_optional = supported_methods.iter().any(|method| {
            // Check return type for optional opaque
            if let ReturnType::Infallible(SuccessType::OutType(Type::Opaque(op))) = &method.output {
                if op.is_optional() {
                    return true;
                }
            }
            // Nullable returns use Optional
            if matches!(&method.output, ReturnType::Nullable(_)) {
                return true;
            }
            // Check params for optional opaque
            method
                .params
                .iter()
                .any(|p| matches!(&p.ty, Type::Opaque(op) if op.is_optional()))
        });

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
            is_error: bool,
            uses_optional: bool,
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
                is_error,
                uses_optional,
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
        if let Some(ref ps) = method.param_self {
            match &ps.ty {
                SelfType::Struct(s) => {
                    let type_id: TypeId = s.tcx_id.into();
                    let type_name = self.formatter.fmt_type_name(type_id);
                    param_layouts.push(format!("{type_name}.LAYOUT"));
                }
                SelfType::Enum(_) => {
                    param_layouts.push("ValueLayout.JAVA_INT".to_string());
                }
                _ => {
                    param_layouts.push("ValueLayout.ADDRESS".to_string());
                }
            }
        }

        // Regular parameters
        for param in method.params.iter() {
            self.push_param_layouts(&param.ty, &mut param_layouts);
        }

        // Write returns pass a write buffer pointer as the last parameter
        let is_write_return = matches!(method.output, ReturnType::Infallible(SuccessType::Write))
            || matches!(
                method.output,
                ReturnType::Fallible(SuccessType::Write, _)
                    | ReturnType::Nullable(SuccessType::Write)
            );
        if is_write_return {
            param_layouts.push("ValueLayout.ADDRESS".to_string());
        }

        // Compute result layout for fallible/nullable returns
        let (result_layout, result_layout_name) = match self.compute_result_layout(method) {
            Some((layout_def, _is_ok_offset)) => {
                let layout_name = format!("{handle_name}_RESULT");
                (Some(layout_def), Some(layout_name))
            }
            None => (None, None),
        };

        // Build the descriptor
        let return_layout =
            self.get_return_layout(method, result_layout_name.as_deref());
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
            result_layout,
            result_layout_name,
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
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                layouts.push(format!("{type_name}.LAYOUT"));
            }
            _ => {
                self.errors
                    .push_error(format!("Unsupported parameter type in Java backend: {ty:?}"));
            }
        }
    }

    fn get_return_layout(
        &self,
        method: &Method,
        result_layout_name: Option<&str>,
    ) -> Option<String> {
        match &method.output {
            ReturnType::Infallible(success) => self.get_success_layout(success),
            ReturnType::Fallible(_, _) | ReturnType::Nullable(_) => {
                // Result/Option returns use a per-method result layout constant
                result_layout_name.map(|n| n.to_string())
            }
        }
    }

    fn get_success_layout(&self, success: &SuccessType) -> Option<String> {
        match success {
            SuccessType::Unit => None,
            SuccessType::OutType(ty) => self.get_type_layout(ty),
            SuccessType::Write => None,
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
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                Some(format!("{type_name}.LAYOUT"))
            }
            _ => {
                self.errors
                    .push_error(format!("Unsupported return type in Java backend: {ty:?}"));
                None
            }
        }
    }

    /// Get the (size, alignment) of the FFI layout for an output type.
    fn get_out_type_size_align(&self, ty: &OutType) -> (usize, usize) {
        out_type_size_align(ty, self.formatter)
    }

    /// Compute the result struct layout string for a fallible/nullable return.
    /// Returns (layout_definition, is_ok_offset) or None for infallible.
    fn compute_result_layout(&self, method: &Method) -> Option<(String, usize)> {
        match &method.output {
            ReturnType::Infallible(_) => None,
            ReturnType::Fallible(ok, err) => {
                let ok_layout = self.get_success_layout(ok);
                let err_layout = err.as_ref().and_then(|e| self.get_type_layout(e));

                let ok_size_align = match ok {
                    SuccessType::OutType(ty) => self.get_out_type_size_align(ty),
                    SuccessType::Write => (0, 0), // Write is handled separately
                    SuccessType::Unit => (0, 0),
                    _ => (0, 0),
                };
                let err_size_align = match err {
                    Some(ty) => self.get_out_type_size_align(ty),
                    None => (0, 0),
                };

                self.build_result_layout_string(
                    ok_layout.as_deref(),
                    err_layout.as_deref(),
                    ok_size_align,
                    err_size_align,
                )
            }
            ReturnType::Nullable(ok) => {
                let ok_layout = self.get_success_layout(ok);
                let ok_size_align = match ok {
                    SuccessType::OutType(ty) => self.get_out_type_size_align(ty),
                    SuccessType::Write => (0, 0),
                    SuccessType::Unit => (0, 0),
                    _ => (0, 0),
                };

                self.build_result_layout_string(ok_layout.as_deref(), None, ok_size_align, (0, 0))
            }
        }
    }

    /// Build the StructLayout definition string for a result type.
    /// The C ABI result layout is: union { Ok ok; Err err; } + bool is_ok;
    /// Returns (layout_string, is_ok_offset).
    fn build_result_layout_string(
        &self,
        ok_layout: Option<&str>,
        err_layout: Option<&str>,
        ok_size_align: (usize, usize),
        err_size_align: (usize, usize),
    ) -> Option<(String, usize)> {
        // Union size is max of ok/err sizes, alignment is max of ok/err alignments
        let union_size = ok_size_align.0.max(err_size_align.0);
        let union_align = ok_size_align.1.max(err_size_align.1).max(1);

        // Pick the layout element for the union (use the larger one, or ok if equal)
        let union_layout = if ok_size_align.0 >= err_size_align.0 {
            ok_layout
        } else {
            err_layout
        };

        let mut members = Vec::new();
        if let Some(layout) = union_layout {
            members.push(format!("{layout}.withName(\"union_val\")"));
            // If the other type is smaller, the union is already sized by the larger
            // But we may need padding after the union to reach union_size
            let used_size = ok_size_align.0.max(err_size_align.0);
            let larger_size = ok_size_align.0.max(err_size_align.0);
            if used_size < larger_size {
                members.push(format!(
                    "MemoryLayout.paddingLayout({})",
                    larger_size - used_size
                ));
            }
        }

        // is_ok field comes after the union, aligned
        let is_ok_offset = if union_size > 0 {
            // Align to 1 (bool alignment)
            union_size
        } else {
            0
        };
        members.push("ValueLayout.JAVA_BOOLEAN.withName(\"is_ok\")".to_string());

        // Trailing padding to align struct to union_align
        let total_before_padding = is_ok_offset + 1; // +1 for bool
        let trailing = (union_align - (total_before_padding % union_align)) % union_align;
        if trailing > 0 {
            members.push(format!("MemoryLayout.paddingLayout({trailing})"));
        }

        let layout_str = format!(
            "MemoryLayout.structLayout(\n            {}\n        )",
            members.join(",\n            ")
        );
        Some((layout_str, is_ok_offset))
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
        // Track string params that need Arena allocation, with their encoding
        let mut string_params: Vec<(String, StringEncoding)> = Vec::new();
        // Track nullable opaque params that need local variable setup
        let mut nullable_setup_lines: Vec<String> = Vec::new();
        // Track if any struct params need arena for toNative
        let mut has_struct_param = false;

        let _has_struct_self = matches!(self_type, Some(SelfType::Struct(_)));
        if let Some(st) = self_type {
            match st {
                SelfType::Struct(_) => {
                    has_struct_param = true;
                    invoke_args.push("this.toNative(arena)".to_string());
                }
                SelfType::Enum(_) => {
                    invoke_args.push("this.toNative()".to_string());
                }
                _ => {
                    invoke_args.push("handle".to_string());
                }
            }
        }

        for param in method.params.iter() {
            let param_name = self.formatter.fmt_param_name(param.name.as_str());
            if matches!(&param.ty, Type::Struct(_)) {
                has_struct_param = true;
            }
            self.gen_java_param(
                &param.ty,
                &param_name,
                &mut java_params,
                &mut invoke_args,
                &mut string_params,
                &mut nullable_setup_lines,
            );
        }

        let is_write_return = matches!(
            method.output,
            ReturnType::Infallible(SuccessType::Write)
                | ReturnType::Fallible(SuccessType::Write, _)
                | ReturnType::Nullable(SuccessType::Write)
        );
        let is_fallible = matches!(method.output, ReturnType::Fallible(_, _));
        let is_nullable = matches!(method.output, ReturnType::Nullable(_));
        let returns_struct = matches!(
            method.output,
            ReturnType::Infallible(SuccessType::OutType(Type::Struct(_)))
        ) || matches!(
            method.output,
            ReturnType::Fallible(SuccessType::OutType(Type::Struct(_)), _)
        ) || matches!(
            method.output,
            ReturnType::Nullable(SuccessType::OutType(Type::Struct(_)))
        );

        if is_write_return {
            invoke_args.push("write".to_string());
        }

        // Determine if we need an arena
        let needs_arena = !string_params.is_empty()
            || has_struct_param
            || returns_struct
            || is_fallible
            || is_nullable;

        // When returning a struct or result layout, FFM prepends SegmentAllocator
        let returns_struct_layout = returns_struct || is_fallible || is_nullable;
        if returns_struct_layout {
            invoke_args.insert(0, "(SegmentAllocator) arena".to_string());
        }

        let java_params_str = java_params.join(", ");
        let args_str = invoke_args.join(", ");
        let nullable_setup = nullable_setup_lines
            .iter()
            .map(|line| format!("            {line}\n"))
            .collect::<String>();

        // Build invoke expression
        let invoke_call = format!("{abi_handle}.invokeExact({args_str})");

        // Build the return statement based on output type
        let return_stmt = if is_fallible || is_nullable {
            self.gen_result_return_stmt(method, &invoke_call, &abi_handle, is_write_return)
        } else if is_write_return {
            format!("{invoke_call};\n            return DiplomatLib.writeToString(write);")
        } else {
            match &method.output {
                ReturnType::Infallible(SuccessType::Unit) => format!("{invoke_call};"),
                ReturnType::Infallible(SuccessType::OutType(ty)) => {
                    if let Type::Opaque(op) = ty {
                        if op.is_optional() {
                            let type_id = ty.id().expect("opaque must have id");
                            let type_name = self.formatter.fmt_type_name(type_id);
                            format!(
                                "var resultAddr = (MemorySegment) {invoke_call};\n            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new {type_name}(resultAddr));"
                            )
                        } else {
                            let wrapped = self.wrap_invoke_result(ty, &invoke_call);
                            format!("return {wrapped};")
                        }
                    } else {
                        let wrapped = self.wrap_invoke_result(ty, &invoke_call);
                        format!("return {wrapped};")
                    }
                }
                _ => "throw new UnsupportedOperationException();".to_string(),
            }
        };

        // Build method body
        let write_setup = if is_write_return {
            "        var write = DiplomatLib.createWrite();\n"
        } else {
            ""
        };
        let body = if needs_arena {
            let mut setup_lines = String::new();
            for (sp, encoding) in &string_params {
                match encoding {
                    StringEncoding::UnvalidatedUtf16 => {
                        setup_lines.push_str(&format!(
                            "            char[] {sp}Chars = {sp}.toCharArray();\n\
                             \n            var {sp}Seg = arena.allocateFrom(ValueLayout.JAVA_CHAR, {sp}Chars);\n"
                        ));
                    }
                    _ => {
                        setup_lines.push_str(&format!(
                            "            byte[] {sp}Bytes = {sp}.getBytes(StandardCharsets.UTF_8);\n\
                             \n            var {sp}Seg = arena.allocateFrom(ValueLayout.JAVA_BYTE, {sp}Bytes);\n"
                        ));
                    }
                }
            }
            format!("{write_setup}        try (var arena = Arena.ofConfined()) {{\n{setup_lines}{nullable_setup}            {return_stmt}\n        }} catch (RuntimeException ex) {{\n            throw ex;\n        }} catch (Throwable ex) {{\n            throw new RuntimeException(ex);\n        }}")
        } else {
            format!("{write_setup}        try {{\n{nullable_setup}            {return_stmt}\n        }} catch (RuntimeException ex) {{\n            throw ex;\n        }} catch (Throwable ex) {{\n            throw new RuntimeException(ex);\n        }}")
        };

        let static_kw = if is_static { "static " } else { "" };

        let definition = format!(
            "public {static_kw}{return_type_java} {method_name}({java_params_str}) {{\n{body}\n    }}"
        );

        JavaMethodInfo { definition }
    }

    /// Generate the return statement for a fallible or nullable method.
    fn gen_result_return_stmt(
        &self,
        method: &Method,
        invoke_call: &str,
        _abi_handle: &str,
        is_write_return: bool,
    ) -> String {
        // Compute is_ok offset
        let is_ok_offset = match self.compute_result_layout(method) {
            Some((_layout, offset)) => offset,
            None => 0,
        };

        let mut lines = Vec::new();
        lines.push(format!("var result = (MemorySegment) {invoke_call};"));
        lines.push(format!(
            "var isOk = result.get(ValueLayout.JAVA_BOOLEAN, {is_ok_offset}L);"
        ));

        match &method.output {
            ReturnType::Fallible(ok, err) => {
                let ok_extract = self.gen_ok_extract(ok, is_write_return);
                let err_throw = self.gen_err_throw(err);
                lines.push("if (isOk) {".to_string());
                lines.push(format!("    {ok_extract}"));
                lines.push("} else {".to_string());
                if is_write_return {
                    lines.push("    DiplomatLib.destroyWrite(write);".to_string());
                }
                lines.push(format!("    {err_throw}"));
                lines.push("}".to_string());
            }
            ReturnType::Nullable(ok) => {
                let ok_extract = self.gen_nullable_ok_extract(ok);
                lines.push("if (isOk) {".to_string());
                lines.push(format!("    {ok_extract}"));
                lines.push("} else {".to_string());
                lines.push("    return Optional.empty();".to_string());
                lines.push("}".to_string());
            }
            _ => {}
        }

        lines.join("\n            ")
    }

    /// Generate the ok-branch extraction for a fallible return.
    fn gen_ok_extract(&self, ok: &SuccessType, is_write_return: bool) -> String {
        if is_write_return {
            return "return DiplomatLib.writeToString(write);".to_string();
        }
        match ok {
            SuccessType::Unit => "return;".to_string(),
            SuccessType::OutType(ty) => {
                let extract = self.gen_result_value_extract(ty, "result");
                format!("return {extract};")
            }
            SuccessType::Write => "return DiplomatLib.writeToString(write);".to_string(),
            _ => "return;".to_string(),
        }
    }

    /// Generate the ok-branch extraction for a nullable return (wrapped in Optional).
    fn gen_nullable_ok_extract(&self, ok: &SuccessType) -> String {
        match ok {
            SuccessType::OutType(ty) => {
                let extract = self.gen_result_value_extract(ty, "result");
                format!("return Optional.of({extract});")
            }
            SuccessType::Write => {
                "return Optional.of(DiplomatLib.writeToString(write));".to_string()
            }
            _ => "return Optional.empty();".to_string(),
        }
    }

    /// Extract a value at offset 0 from a result MemorySegment.
    fn gen_result_value_extract(&self, ty: &OutType, seg_name: &str) -> String {
        match ty {
            Type::Primitive(prim) => {
                let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                let cast = self.formatter.fmt_primitive_as_java(*prim);
                format!("({cast}) {seg_name}.get({layout}, 0L)")
            }
            Type::Opaque(_) => {
                let type_id = ty.id().expect("opaque must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!("new {type_name}({seg_name}.get(ValueLayout.ADDRESS, 0L))")
            }
            Type::Enum(_) => {
                let type_id = ty.id().expect("enum must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!("{type_name}.fromNative((int) {seg_name}.get(ValueLayout.JAVA_INT, 0L))")
            }
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!(
                    "{type_name}.fromNative({seg_name}.asSlice(0L, {type_name}.LAYOUT.byteSize()))"
                )
            }
            _ => "null".to_string(),
        }
    }

    /// Generate the error throw statement for a fallible return.
    fn gen_err_throw(&self, err: &Option<OutType>) -> String {
        match err {
            None => {
                // Result<T, ()> — unit error
                "throw new RuntimeException(\"Diplomat error\");".to_string()
            }
            Some(err_ty) => match err_ty {
                Type::Enum(_) => {
                    let type_id = err_ty.id().expect("enum must have id");
                    let type_name = self.formatter.fmt_type_name(type_id);
                    let resolved = self.formatter.tcx().resolve_type(type_id);
                    if resolved.attrs().custom_errors {
                        format!("throw new {type_name}Exception({type_name}.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L)));")
                    } else {
                        format!("throw new RuntimeException(\"{type_name} error: \" + {type_name}.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L)));")
                    }
                }
                Type::Opaque(_) => {
                    let type_id = err_ty.id().expect("opaque must have id");
                    let type_name = self.formatter.fmt_type_name(type_id);
                    format!(
                        "throw new {type_name}(result.get(ValueLayout.ADDRESS, 0L));"
                    )
                }
                Type::Struct(_) => {
                    let type_id = err_ty.id().expect("struct must have id");
                    let type_name = self.formatter.fmt_type_name(type_id);
                    format!(
                        "throw {type_name}.fromNative(result.asSlice(0L, {type_name}.LAYOUT.byteSize()));"
                    )
                }
                Type::Primitive(prim) => {
                    let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                    let cast = self.formatter.fmt_primitive_as_java(*prim);
                    format!(
                        "throw new RuntimeException(\"Diplomat error: \" + ({cast}) result.get({layout}, 0L));"
                    )
                }
                _ => "throw new RuntimeException(\"Diplomat error\");".to_string(),
            },
        }
    }

    fn gen_java_param<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        param_name: &str,
        java_params: &mut Vec<String>,
        invoke_args: &mut Vec<String>,
        string_params: &mut Vec<(String, StringEncoding)>,
        nullable_setup_lines: &mut Vec<String>,
    ) {
        match ty {
            Type::Primitive(prim) => {
                let java_type = self.formatter.fmt_primitive_as_java(*prim);
                java_params.push(format!("{java_type} {param_name}"));
                invoke_args.push(param_name.to_string());
            }
            Type::Opaque(op) => {
                let type_name: Cow<str> = ty
                    .id()
                    .map(|id| self.formatter.fmt_type_name(id))
                    .unwrap_or("MemorySegment".into());
                java_params.push(format!("{type_name} {param_name}"));
                if op.is_optional() {
                    let local_var = format!("{param_name}Addr");
                    nullable_setup_lines.push(format!(
                        "MemorySegment {local_var} = {param_name} == null ? MemorySegment.NULL : {param_name}.handle;"
                    ));
                    invoke_args.push(local_var);
                } else {
                    invoke_args.push(format!("{param_name}.handle"));
                }
            }
            Type::Slice(Slice::Str(_, encoding)) => {
                java_params.push(format!("String {param_name}"));
                string_params.push((param_name.to_string(), *encoding));
                match encoding {
                    StringEncoding::UnvalidatedUtf16 => {
                        invoke_args.push(format!("{param_name}Seg"));
                        invoke_args.push(format!("(long) {param_name}Chars.length"));
                    }
                    _ => {
                        invoke_args.push(format!("{param_name}Seg"));
                        invoke_args.push(format!("(long) {param_name}Bytes.length"));
                    }
                }
            }
            Type::Enum(_) => {
                let type_name: Cow<str> = ty
                    .id()
                    .map(|id| self.formatter.fmt_type_name(id))
                    .unwrap_or("int".into());
                java_params.push(format!("{type_name} {param_name}"));
                invoke_args.push(format!("{param_name}.toNative()"));
            }
            Type::Struct(_) => {
                let type_name: Cow<str> = ty
                    .id()
                    .map(|id| self.formatter.fmt_type_name(id))
                    .unwrap_or("MemorySegment".into());
                java_params.push(format!("{type_name} {param_name}"));
                invoke_args.push(format!("{param_name}.toNative(arena)"));
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
            ReturnType::Fallible(ok, _err) => {
                // Errors are thrown, so return type is just the ok type
                match ok {
                    SuccessType::Unit => "void".to_string(),
                    SuccessType::OutType(ty) => self.gen_out_type_java(ty, owner_type_name),
                    SuccessType::Write => "String".to_string(),
                    _ => "void".to_string(),
                }
            }
            ReturnType::Nullable(ok) => {
                // Nullable returns use Optional<T>
                match ok {
                    SuccessType::OutType(ty) => {
                        let inner = self.gen_out_type_java_boxed(ty, owner_type_name);
                        format!("Optional<{inner}>")
                    }
                    SuccessType::Write => "Optional<String>".to_string(),
                    _ => "Optional<Void>".to_string(),
                }
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
                    format!("Optional<{type_name}>")
                } else {
                    type_name.to_string()
                }
            }
            Type::Enum(_) => {
                let type_id = ty.id().expect("enum must have id");
                self.formatter.fmt_type_name(type_id).to_string()
            }
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                self.formatter.fmt_type_name(type_id).to_string()
            }
            _ => {
                self.errors.push_error(format!(
                    "Unsupported return type in Java backend for {owner_type_name}: {ty:?}"
                ));
                "Object".to_string()
            }
        }
    }

    /// Like gen_out_type_java but returns boxed types for primitives (for use in generics).
    fn gen_out_type_java_boxed(&self, ty: &OutType, owner_type_name: &str) -> String {
        match ty {
            Type::Primitive(prim) => {
                self.formatter.fmt_primitive_as_java_boxed(*prim).to_string()
            }
            _ => self.gen_out_type_java(ty, owner_type_name),
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
            Type::Enum(_) => {
                let type_id = ty.id().expect("enum must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!("{type_name}.fromNative((int) {invoke_call})")
            }
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!(
                    "{type_name}.fromNative((MemorySegment) {invoke_call})"
                )
            }
            _ => invoke_call.to_string(),
        }
    }

    fn gen_enum_def(
        &self,
        ty: &'cx hir::EnumDef,
        type_name: &str,
        is_error: bool,
    ) -> (String, String) {
        let variants: Vec<JavaEnumVariantInfo> = ty
            .variants
            .iter()
            .map(|v| JavaEnumVariantInfo {
                variant_name: self.formatter.fmt_enum_variant_name(v).to_string(),
                discriminant: v.discriminant,
            })
            .collect();

        let supported_methods: Vec<&Method> = ty
            .methods
            .iter()
            .filter(|m| !m.attrs.disable && self.is_method_supported(m))
            .collect();

        let uses_optional = supported_methods.iter().any(|method| {
            if let ReturnType::Infallible(SuccessType::OutType(Type::Opaque(op))) = &method.output {
                if op.is_optional() {
                    return true;
                }
            }
            if matches!(&method.output, ReturnType::Nullable(_)) {
                return true;
            }
            method
                .params
                .iter()
                .any(|p| matches!(&p.ty, Type::Opaque(op) if op.is_optional()))
        });

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
        #[template(path = "java/Enum.java.jinja", escape = "none")]
        struct EnumTemplate<'a> {
            domain: &'a str,
            lib_name: &'a str,
            dylib_name: &'a str,
            type_name: &'a str,
            is_error: bool,
            uses_optional: bool,
            variants: &'a [JavaEnumVariantInfo],
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
            EnumTemplate {
                domain: self.domain,
                lib_name: self.lib_name,
                dylib_name: self.dylib_name,
                type_name,
                is_error,
                uses_optional,
                variants: &variants,
                native_methods: &native_methods,
                companion_methods: &companion_methods,
                self_methods: &self_methods,
            }
            .render()
            .expect("failed to render enum type"),
        )
    }

    fn gen_struct_def<P: hir::TyPosition + 'cx>(
        &self,
        ty: &'cx hir::StructDef<P>,
        type_name: &str,
        is_out_struct: bool,
    ) -> (String, String) {
        let is_error = ty.attrs.custom_errors;

        // Compute field info
        let fields: Vec<JavaStructFieldInfo> =
            self.compute_struct_fields(&ty.fields, type_name);

        // Compute layout members string with padding
        let layout_members = self.compute_layout_members(&ty.fields, type_name);

        // Filter supported methods
        let supported_methods: Vec<&Method> = ty
            .methods
            .iter()
            .filter(|m| !m.attrs.disable && self.is_method_supported(m))
            .collect();

        let uses_optional = supported_methods.iter().any(|method| {
            if let ReturnType::Infallible(SuccessType::OutType(Type::Opaque(op))) = &method.output {
                if op.is_optional() {
                    return true;
                }
            }
            if matches!(&method.output, ReturnType::Nullable(_)) {
                return true;
            }
            method
                .params
                .iter()
                .any(|p| matches!(&p.ty, Type::Opaque(op) if op.is_optional()))
        });

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
        #[template(path = "java/Struct.java.jinja", escape = "none")]
        struct StructTemplate<'a> {
            domain: &'a str,
            lib_name: &'a str,
            dylib_name: &'a str,
            type_name: &'a str,
            is_error: bool,
            is_out_struct: bool,
            uses_optional: bool,
            layout_members: &'a str,
            fields: &'a [JavaStructFieldInfo],
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
            StructTemplate {
                domain: self.domain,
                lib_name: self.lib_name,
                dylib_name: self.dylib_name,
                type_name,
                is_error,
                is_out_struct,
                uses_optional,
                layout_members: &layout_members,
                fields: &fields,
                native_methods: &native_methods,
                companion_methods: &companion_methods,
                self_methods: &self_methods,
            }
            .render()
            .expect("failed to render struct type"),
        )
    }

    /// Compute Java field info for each struct field.
    fn compute_struct_fields<P: hir::TyPosition>(
        &self,
        fields: &[StructField<P>],
        _type_name: &str,
    ) -> Vec<JavaStructFieldInfo> {
        // First compute offsets for each field (needed for fromNative/toNative with nested structs)
        let mut offset: usize = 0;
        let mut field_offsets = Vec::new();
        for field in fields.iter() {
            let (f_size, f_align) = self.get_field_size_align(&field.ty);
            if f_align > 0 {
                let padding = (f_align - (offset % f_align)) % f_align;
                offset += padding;
            }
            field_offsets.push(offset);
            offset += f_size;
        }

        fields
            .iter()
            .zip(field_offsets.iter())
            .map(|(field, &field_offset)| {
                let field_name = self.formatter.fmt_field_name(field.name.as_str()).to_string();
                let java_type = self.field_java_type(&field.ty);
                let from_native_expr = self.field_from_native(&field.ty, &field_name, field_offset);
                let to_native_stmt = self.field_to_native(&field.ty, &field_name, field_offset);

                JavaStructFieldInfo {
                    field_name,
                    java_type,
                    from_native_expr,
                    to_native_stmt,
                }
            })
            .collect()
    }

    /// Compute the StructLayout members string with padding for a struct's fields.
    fn compute_layout_members<P: hir::TyPosition>(
        &self,
        fields: &[StructField<P>],
        _type_name: &str,
    ) -> String {
        let mut members = Vec::new();
        let mut offset: usize = 0;
        let mut max_align: usize = 1;

        for field in fields.iter() {
            let field_name = self.formatter.fmt_field_name(field.name.as_str());
            let (f_size, f_align) = self.get_field_size_align(&field.ty);
            let layout_element = self.field_layout_element(&field.ty);

            if f_align > 0 {
                let padding = (f_align - (offset % f_align)) % f_align;
                if padding > 0 {
                    members.push(format!("MemoryLayout.paddingLayout({padding})"));
                    offset += padding;
                }
            }

            members.push(format!("{layout_element}.withName(\"{field_name}\")"));
            offset += f_size;
            if f_align > max_align {
                max_align = f_align;
            }
        }

        // Trailing padding
        if max_align > 0 {
            let trailing = (max_align - (offset % max_align)) % max_align;
            if trailing > 0 {
                members.push(format!("MemoryLayout.paddingLayout({trailing})"));
            }
        }

        if members.is_empty() {
            // ZST: structLayout requires at least one element
            "MemoryLayout.paddingLayout(0)".to_string()
        } else {
            members.join(",\n        ")
        }
    }

    fn get_field_size_align<P: hir::TyPosition>(&self, ty: &Type<P>) -> (usize, usize) {
        field_size_align_generic(ty, self.formatter)
    }

    fn field_layout_element<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
        match ty {
            Type::Primitive(prim) => self.formatter.fmt_primitive_as_ffi(*prim).to_string(),
            Type::Opaque(_) => "ValueLayout.ADDRESS".to_string(),
            Type::Enum(_) => "ValueLayout.JAVA_INT".to_string(),
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!("{type_name}.LAYOUT")
            }
            Type::Slice(_) => "DiplomatLib.DIPLOMAT_STRING_VIEW".to_string(),
            Type::DiplomatOption(inner) => {
                let inner_layout = self.field_layout_element(inner);
                let (_inner_size, inner_align) = field_size_align_generic(inner.as_ref(), self.formatter);
                let padding_after_bool = if inner_align > 1 { inner_align - 1 } else { 0 };
                if padding_after_bool > 0 {
                    format!("MemoryLayout.structLayout({inner_layout}, ValueLayout.JAVA_BOOLEAN, MemoryLayout.paddingLayout({padding_after_bool}))")
                } else {
                    format!("MemoryLayout.structLayout({inner_layout}, ValueLayout.JAVA_BOOLEAN)")
                }
            }
            _ => "ValueLayout.JAVA_BYTE".to_string(),
        }
    }

    fn field_java_type<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
        match ty {
            Type::Primitive(prim) => self.formatter.fmt_primitive_as_java(*prim).to_string(),
            Type::Opaque(_) => {
                let type_id = ty.id().expect("opaque must have id");
                self.formatter.fmt_type_name(type_id).to_string()
            }
            Type::Enum(_) => {
                let type_id = ty.id().expect("enum must have id");
                self.formatter.fmt_type_name(type_id).to_string()
            }
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                self.formatter.fmt_type_name(type_id).to_string()
            }
            Type::Slice(slc) => match slc {
                Slice::Str(_, _) => "String".to_string(),
                Slice::Primitive(_, prim) => {
                    format!("{}[]", self.formatter.fmt_primitive_as_java(*prim))
                }
                _ => "Object".to_string(),
            },
            Type::DiplomatOption(inner) => {
                // Use boxed/nullable types for optional wrapper
                match inner.as_ref() {
                    Type::Primitive(prim) => {
                        self.formatter.fmt_primitive_as_java_boxed(*prim).to_string()
                    }
                    Type::Enum(_) => {
                        let type_id = inner.id().expect("enum must have id");
                        self.formatter.fmt_type_name(type_id).to_string()
                    }
                    Type::Struct(_) => {
                        let type_id = inner.id().expect("struct must have id");
                        self.formatter.fmt_type_name(type_id).to_string()
                    }
                    Type::Slice(slc) => match slc {
                        Slice::Str(_, _) => "String".to_string(),
                        Slice::Primitive(_, prim) => {
                            format!("{}[]", self.formatter.fmt_primitive_as_java(*prim))
                        }
                        _ => "Object".to_string(),
                    },
                    _ => "Object".to_string(),
                }
            }
            _ => "Object".to_string(),
        }
    }

    /// Generate the expression to read a field from a MemorySegment in fromNative.
    fn field_from_native<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        field_name: &str,
        offset: usize,
    ) -> String {
        match ty {
            Type::Primitive(prim) => {
                let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                format!("({java_type}) seg.get({layout}, {offset}L)", java_type = self.formatter.fmt_primitive_as_java(*prim))
            }
            Type::Opaque(_) => {
                let type_id = ty.id().expect("opaque must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!("new {type_name}(seg.get(ValueLayout.ADDRESS, {offset}L))")
            }
            Type::Enum(_) => {
                let type_id = ty.id().expect("enum must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!("{type_name}.fromNative((int) seg.get(ValueLayout.JAVA_INT, {offset}L))")
            }
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!(
                    "{type_name}.fromNative(seg.asSlice({offset}L, {type_name}.LAYOUT.byteSize()))"
                )
            }
            Type::Slice(slc) => {
                let data_offset = offset;
                let len_offset = offset + 8;
                match slc {
                    Slice::Str(_, encoding) => {
                        let charset = match encoding {
                            StringEncoding::UnvalidatedUtf16 => "StandardCharsets.UTF_16LE",
                            _ => "StandardCharsets.UTF_8",
                        };
                        let byte_multiplier = match encoding {
                            StringEncoding::UnvalidatedUtf16 => " * 2",
                            _ => "",
                        };
                        format!(
                            "new String(seg.get(ValueLayout.ADDRESS, {data_offset}L).reinterpret(seg.get(ValueLayout.JAVA_LONG, {len_offset}L){byte_multiplier}).toArray(ValueLayout.JAVA_BYTE), {charset})"
                        )
                    }
                    Slice::Primitive(_, prim) => {
                        let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                        let (elem_size, _) = self.formatter.primitive_size_align(*prim);
                        format!(
                            "seg.get(ValueLayout.ADDRESS, {data_offset}L).reinterpret(seg.get(ValueLayout.JAVA_LONG, {len_offset}L) * {elem_size}L).toArray({layout})"
                        )
                    }
                    _ => format!("null /* unsupported slice field {field_name} */"),
                }
            }
            Type::DiplomatOption(inner) => {
                let (inner_size, _inner_align) = field_size_align_generic(inner.as_ref(), self.formatter);
                let is_ok_offset = offset + inner_size;
                let inner_expr = self.field_from_native(inner.as_ref(), field_name, offset);
                format!("seg.get(ValueLayout.JAVA_BOOLEAN, {is_ok_offset}L) ? {inner_expr} : null")
            }
            _ => format!("null /* unsupported field {field_name} */"),
        }
    }

    /// Generate the statement to write a field to a MemorySegment in toNative.
    fn field_to_native<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        field_name: &str,
        offset: usize,
    ) -> String {
        match ty {
            Type::Primitive(prim) => {
                let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                format!("seg.set({layout}, {offset}L, this.{field_name});")
            }
            Type::Opaque(_) => {
                format!("seg.set(ValueLayout.ADDRESS, {offset}L, this.{field_name}.handle);")
            }
            Type::Enum(_) => {
                format!("seg.set(ValueLayout.JAVA_INT, {offset}L, this.{field_name}.toNative());")
            }
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!(
                    "seg.asSlice({offset}L, {type_name}.LAYOUT.byteSize()).copyFrom(this.{field_name}.toNative(arena));"
                )
            }
            Type::Slice(slc) => {
                let data_offset = offset;
                let len_offset = offset + 8;
                match slc {
                    Slice::Str(_, encoding) => {
                        let (charset, elem_layout) = match encoding {
                            StringEncoding::UnvalidatedUtf16 => ("StandardCharsets.UTF_16LE", "ValueLayout.JAVA_BYTE"),
                            _ => ("StandardCharsets.UTF_8", "ValueLayout.JAVA_BYTE"),
                        };
                        let len_expr = match encoding {
                            StringEncoding::UnvalidatedUtf16 => format!("{field_name}Bytes.length / 2"),
                            _ => format!("{field_name}Bytes.length"),
                        };
                        format!(
                            "{{ byte[] {field_name}Bytes = this.{field_name}.getBytes({charset}); var {field_name}Seg = arena.allocateFrom({elem_layout}, {field_name}Bytes); seg.set(ValueLayout.ADDRESS, {data_offset}L, {field_name}Seg); seg.set(ValueLayout.JAVA_LONG, {len_offset}L, (long) {len_expr}); }}"
                        )
                    }
                    Slice::Primitive(_, prim) => {
                        let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                        format!(
                            "{{ var {field_name}Seg = arena.allocateFrom({layout}, this.{field_name}); seg.set(ValueLayout.ADDRESS, {data_offset}L, {field_name}Seg); seg.set(ValueLayout.JAVA_LONG, {len_offset}L, (long) this.{field_name}.length); }}"
                        )
                    }
                    _ => format!("// unsupported slice field {field_name}"),
                }
            }
            Type::DiplomatOption(inner) => {
                let (inner_size, _inner_align) = field_size_align_generic(inner.as_ref(), self.formatter);
                let is_ok_offset = offset + inner_size;
                let inner_to_native = self.field_to_native_value(inner.as_ref(), field_name, offset);
                format!(
                    "if (this.{field_name} != null) {{ {inner_to_native} seg.set(ValueLayout.JAVA_BOOLEAN, {is_ok_offset}L, true); }} else {{ seg.set(ValueLayout.JAVA_BOOLEAN, {is_ok_offset}L, false); }}"
                )
            }
            _ => format!("// unsupported field {field_name}"),
        }
    }

    /// Generate the statement to write a field's inner value to a MemorySegment (for Option unwrapping).
    fn field_to_native_value<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        field_name: &str,
        offset: usize,
    ) -> String {
        match ty {
            Type::Primitive(prim) => {
                let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                format!("seg.set({layout}, {offset}L, this.{field_name});")
            }
            Type::Enum(_) => {
                format!("seg.set(ValueLayout.JAVA_INT, {offset}L, this.{field_name}.toNative());")
            }
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                let type_name = self.formatter.fmt_type_name(type_id);
                format!(
                    "seg.asSlice({offset}L, {type_name}.LAYOUT.byteSize()).copyFrom(this.{field_name}.toNative(arena));"
                )
            }
            Type::Slice(slc) => {
                let data_offset = offset;
                let len_offset = offset + 8;
                match slc {
                    Slice::Str(_, encoding) => {
                        let charset = match encoding {
                            StringEncoding::UnvalidatedUtf16 => "StandardCharsets.UTF_16LE",
                            _ => "StandardCharsets.UTF_8",
                        };
                        let len_expr = match encoding {
                            StringEncoding::UnvalidatedUtf16 => format!("{field_name}Bytes.length / 2"),
                            _ => format!("{field_name}Bytes.length"),
                        };
                        format!(
                            "byte[] {field_name}Bytes = this.{field_name}.getBytes({charset}); var {field_name}Seg = arena.allocateFrom(ValueLayout.JAVA_BYTE, {field_name}Bytes); seg.set(ValueLayout.ADDRESS, {data_offset}L, {field_name}Seg); seg.set(ValueLayout.JAVA_LONG, {len_offset}L, (long) {len_expr});"
                        )
                    }
                    Slice::Primitive(_, prim) => {
                        let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                        format!(
                            "var {field_name}Seg = arena.allocateFrom({layout}, this.{field_name}); seg.set(ValueLayout.ADDRESS, {data_offset}L, {field_name}Seg); seg.set(ValueLayout.JAVA_LONG, {len_offset}L, (long) this.{field_name}.length);"
                        )
                    }
                    _ => format!("// unsupported option inner slice field {field_name}"),
                }
            }
            _ => format!("// unsupported option inner field {field_name}"),
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
                let (_file, body) = cx.gen_opaque_def(o, o.name.as_str(), false);
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

    #[test]
    fn test_opaque_with_utf16_string() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct Utf16Wrap(());

                impl Utf16Wrap {
                    pub fn from_utf16(input: &DiplomatStr16) -> Box<Utf16Wrap> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_opaque_with_optional_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct MyData(());

                impl MyData {
                    pub fn create(v: i32) -> Option<Box<MyData>> {
                        unimplemented!()
                    }

                    pub fn get_value(&self) -> i32 {
                        unimplemented!()
                    }

                    pub fn check(data: Option<&MyData>) -> bool {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_opaque_with_write_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                use diplomat_runtime::DiplomatWrite;

                #[diplomat::opaque]
                struct MyString(());

                impl MyString {
                    pub fn get_str(&self, write: &mut DiplomatWrite) {
                        unimplemented!()
                    }

                    pub fn string_transform(foo: &DiplomatStr, write: &mut DiplomatWrite) {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    fn gen_struct_for_test(tk_stream: proc_macro2::TokenStream) -> String {
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
            match ty {
                TypeDef::Struct(s) => {
                    let (_file, body) = cx.gen_struct_def(s, s.name.as_str(), false);
                    result.push_str(&body);
                }
                TypeDef::OutStruct(s) => {
                    let (_file, body) = cx.gen_struct_def(s, s.name.as_str(), true);
                    result.push_str(&body);
                }
                _ => {}
            }
        }
        result
    }

    fn gen_all_for_test(tk_stream: proc_macro2::TokenStream) -> String {
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
            match ty {
                TypeDef::Opaque(o) => {
                    let (_file, body) = cx.gen_opaque_def(o, o.name.as_str(), o.attrs.custom_errors);
                    result.push_str(&body);
                    result.push('\n');
                }
                TypeDef::Struct(s) => {
                    let (_file, body) = cx.gen_struct_def(s, s.name.as_str(), false);
                    result.push_str(&body);
                    result.push('\n');
                }
                TypeDef::OutStruct(s) => {
                    let (_file, body) = cx.gen_struct_def(s, s.name.as_str(), true);
                    result.push_str(&body);
                    result.push('\n');
                }
                TypeDef::Enum(e) => {
                    let (_file, body) = cx.gen_enum_def(e, e.name.as_str(), e.attrs.custom_errors);
                    result.push_str(&body);
                    result.push('\n');
                }
                _ => {}
            }
        }
        result
    }

    #[test]
    fn test_simple_struct() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct MyStruct {
                    a: u8,
                    b: bool,
                    c: u64,
                    d: i32,
                }

                impl MyStruct {
                    pub fn new(a: u8, b: bool, c: u64, d: i32) -> MyStruct {
                        unimplemented!()
                    }

                    pub fn into_a(self) -> u8 {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_struct_for_test(tk_stream));
    }

    #[test]
    fn test_nested_struct() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct Inner {
                    x: i32,
                    y: i32,
                }

                pub struct Outer {
                    inner: Inner,
                    z: f64,
                }
            }
        };

        insta::assert_snapshot!(gen_struct_for_test(tk_stream));
    }

    #[test]
    fn test_fallible_int_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct Foo(());

                impl Foo {
                    pub fn try_int(i: i32) -> Result<i32, ()> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_fallible_opaque_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct Foo(());

                impl Foo {
                    pub fn try_new(i: i32) -> Result<Box<Foo>, ()> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_fallible_with_struct_error() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::attr(auto, error)]
                pub struct ErrorStruct {
                    i: i32,
                    j: i32,
                }

                #[diplomat::opaque]
                struct Foo(());

                impl Foo {
                    pub fn try_new(i: i32) -> Result<Box<Foo>, ErrorStruct> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_fallible_void_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct Foo(());

                impl Foo {
                    pub fn do_thing(&self) -> Result<(), ()> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_nullable_struct_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::out]
                pub struct MyStruct {
                    a: i32,
                    b: bool,
                }

                #[diplomat::opaque]
                struct Foo(());

                impl Foo {
                    pub fn get_struct(&self) -> Option<MyStruct> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_struct_param() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct Config {
                    width: i32,
                    height: i32,
                }

                #[diplomat::opaque]
                struct Renderer(());

                impl Renderer {
                    pub fn new(config: Config) -> Box<Renderer> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_fallible_write_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                use diplomat_runtime::DiplomatWrite;

                #[diplomat::opaque]
                struct Foo(());

                impl Foo {
                    pub fn try_to_string(&self, write: &mut DiplomatWrite) -> Result<(), ()> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_simple_enum() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub enum Color {
                    Red = 0,
                    Green = 1,
                    Blue = 2,
                }

                impl Color {
                    pub fn flip(&self) -> Color {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_enum_as_error() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::attr(auto, error)]
                pub enum ErrorCode {
                    NotFound = 0,
                    PermissionDenied = 1,
                    Internal = 2,
                }

                #[diplomat::opaque]
                struct Foo(());

                impl Foo {
                    pub fn try_new(i: i32) -> Result<Box<Foo>, ErrorCode> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_enum_param_and_return() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub enum Season {
                    Spring = 0,
                    Summer = 1,
                    Autumn = 2,
                    Winter = 3,
                }

                #[diplomat::opaque]
                struct Weather(());

                impl Weather {
                    pub fn from_season(season: Season) -> Box<Weather> {
                        unimplemented!()
                    }

                    pub fn get_season(&self) -> Season {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_struct_with_enum_field() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub enum Direction {
                    North = 0,
                    South = 1,
                    East = 2,
                    West = 3,
                }

                pub struct Movement {
                    direction: Direction,
                    distance: f64,
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_struct_with_slice_fields() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct SliceFields<'a> {
                    name: DiplomatStrSlice<'a>,
                    data: DiplomatSlice<'a, u16>,
                }
            }
        };

        insta::assert_snapshot!(gen_struct_for_test(tk_stream));
    }

    #[test]
    fn test_struct_with_option_fields() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub enum MyEnum {
                    A = 0,
                    B = 1,
                }

                pub struct OptionFields {
                    a: DiplomatOption<u8>,
                    b: DiplomatOption<MyEnum>,
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }
}
