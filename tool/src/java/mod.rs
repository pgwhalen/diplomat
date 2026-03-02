use askama::Template;
use diplomat_core::hir::{
    self, BackendAttrSupport, Callback, CallbackInstantiationFunctionality, DocsUrlGenerator,
    InputOnly, Method, OutType, ReturnType, SelfType, Slice, SpecialMethod, StringEncoding,
    StructField, SuccessType, TraitIdGetter, Type, TypeContext, TypeDef, TypeId,
};
use heck::{ToLowerCamelCase, ToShoutySnakeCase, ToUpperCamelCase};
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

    a.constructors = true;
    a.named_constructors = true;
    a.fallible_constructors = true;
    a.accessors = false;
    a.static_accessors = false;
    a.stringifiers = false;
    a.comparators = false;
    a.iterators = true;
    a.iterables = true;
    a.indexing = true;
    a.callbacks = true;
    a.traits = true;
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

    // Generate trait files
    for (_id, trt_def) in tcx.all_traits() {
        let _guard = ty_gen_cx
            .errors
            .set_context_ty(trt_def.name.as_str().into());
        if trt_def.attrs.disable {
            continue;
        }
        let trait_name = trt_def.name.to_string();
        let (file_name, body) = ty_gen_cx.gen_trait_def(trt_def, &trait_name);
        files.add_file(file_name, body);
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
    docs: String,
    definition: String,
    /// Callback functional interface declarations to put at class level
    callback_interfaces: Vec<String>,
    /// Callback static runner methods to put at class level
    callback_runners: Vec<String>,
    /// Callback static field + static initializer code to put at class level
    callback_statics: Vec<String>,
}

struct JavaNativeMethodInfo {
    handle_name: String,
    abi_name: String,
    descriptor: String,
    result_layout: Option<String>,
    result_layout_name: Option<String>,
}

struct JavaStructFieldInfo {
    docs: String,
    field_name: String,
    java_type: String,
    from_native_expr: String,
    to_native_stmt: String,
}

struct JavaVarHandleInfo {
    handle_name: String,
    declaration: String,
}

struct JavaOffsetConstInfo {
    const_name: String,
    declaration: String,
}

/// Java type, native (FFI) type, layout, and conversion expression for a callback param.
struct CallbackParamTypeInfo {
    java_type: String,
    native_type: String,
    layout: String,
    conversion: String,
}

/// Java return type, layout, and whether it's void for a callback/trait return type.
struct ReturnTypeInfo {
    java_type: String,
    layout: Option<String>,
    is_void: bool,
}

/// Info about a callback parameter in a method, used to generate functional interface,
/// runner method, upcall stub, and setup code in the method body.
struct JavaCallbackInfo {
    /// Unique name for this callback (e.g. "testMultiArgCallback_f")
    unique_name: String,
    /// Java parameter name (e.g. "f")
    param_name: String,
    /// Name of the functional interface (e.g. "TestMultiArgCallbackF")
    interface_name: String,
    /// Java parameter types for the functional interface (e.g. "int arg0")
    interface_params: Vec<String>,
    /// Java return type for the functional interface
    interface_return_type: String,
    /// Runner method parameter types: native types including MemorySegment data
    runner_native_params: Vec<String>,
    /// Expressions to convert native args to Java types inside the runner
    runner_arg_conversions: Vec<String>,
    /// FunctionDescriptor parameter layouts (not including ADDRESS for data)
    param_layouts: Vec<String>,
    /// FunctionDescriptor return layout (None for void)
    return_layout: Option<String>,
    /// Whether the callback returns void
    returns_void: bool,
}

struct JavaEnumVariantInfo {
    docs: String,
    variant_name: String,
    discriminant: isize,
}

#[derive(Default)]
struct JavaSpecialMethods {
    /// Boxed yield type for Iterator<T> (e.g. "Byte", "AttrOpaque1Renamed")
    iterator_type: Option<String>,
    /// Indexer info for get() wrapper
    indexer_type: Option<JavaIndexerType>,
    /// Concrete iterator class name (e.g. "MyIterator")
    iterable_type: Option<String>,
    /// Boxed item type for Iterable<T> (resolved from the iterator's yield type)
    iterable_item_type: Option<String>,
}

struct JavaIndexerType {
    index_type: String,
    item_type: String,
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
    /// Resolve a type's id and format its name as a String.
    fn fmt_type_name_str<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
        let type_id = ty.id().expect("type must have id");
        self.formatter.fmt_type_name(type_id).to_string()
    }

    /// Get the Java type name for a type (primitives, enums, structs, opaques).
    fn type_to_java_name<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
        match ty {
            Type::Primitive(prim) => self.formatter.fmt_primitive_as_java(*prim).to_string(),
            Type::Enum(_) | Type::Struct(_) | Type::Opaque(_) => self.fmt_type_name_str(ty),
            Type::Slice(slice) => match slice {
                Slice::Str(_, _) => "String".to_string(),
                Slice::Primitive(_, prim) => {
                    format!("{}[]", self.formatter.fmt_primitive_as_java(*prim))
                }
                _ => "Object".to_string(),
            },
            _ => "Object".to_string(),
        }
    }

    /// Check if any methods use Optional (optional opaque return/params, nullable returns).
    fn methods_use_optional(&self, methods: &[&Method]) -> bool {
        methods.iter().any(|method| {
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
        })
    }

    /// Get (size, alignment) for any Type<P>.
    fn field_size_align<P: hir::TyPosition>(&self, ty: &Type<P>) -> (usize, usize) {
        match ty {
            Type::Primitive(prim) => self.formatter.primitive_size_align(*prim),
            Type::Opaque(_) => (8, 8),
            Type::Enum(_) => (4, 4),
            Type::Struct(_) => {
                let type_id = ty.id().expect("struct must have id");
                self.type_size_align_by_id(type_id)
            }
            Type::Slice(_) => (16, 8),
            Type::DiplomatOption(inner) => {
                let (inner_size, inner_align) = self.field_size_align(inner);
                if inner_align == 0 {
                    return (0, 0);
                }
                let total = align_up(inner_size + 1, inner_align);
                (total, inner_align)
            }
            _ => (0, 0),
        }
    }

    /// Compute (size, alignment) for a type by its TypeId.
    fn type_size_align_by_id(&self, type_id: TypeId) -> (usize, usize) {
        let resolved = self.formatter.tcx().resolve_type(type_id);
        match resolved {
            TypeDef::Struct(s) => compute_struct_fields_size_align(
                s.fields.iter().map(|f| self.field_size_align(&f.ty)),
            ),
            TypeDef::OutStruct(s) => compute_struct_fields_size_align(
                s.fields.iter().map(|f| self.field_size_align(&f.ty)),
            ),
            TypeDef::Opaque(_) => (8, 8),
            TypeDef::Enum(_) => (4, 4),
            _ => (0, 0),
        }
    }

    /// Core type-to-FFI-layout mapping shared by layout computation functions.
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

    /// Generate the file path for a Java type file.
    fn java_file_path(&self, type_name: &str) -> String {
        format!(
            "src/main/java/{}/{}/{type_name}.java",
            self.domain.replace('.', "/"),
            self.lib_name,
        )
    }

    /// Get the MethodType class literal for a type.
    fn type_to_method_type_class<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
        match ty {
            Type::Primitive(prim) => {
                format!("{}.class", self.formatter.fmt_primitive_as_java(*prim))
            }
            Type::Enum(_) => "int.class".to_string(),
            Type::Struct(_) | Type::Opaque(_) => "MemorySegment.class".to_string(),
            _ => "MemorySegment.class".to_string(),
        }
    }

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
                | Type::Slice(Slice::Primitive(_, _))
                | Type::Enum(_)
                | Type::Struct(_)
                | Type::Callback(_)
                | Type::ImplTrait(_)
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

        // Scan for special methods (iterator, iterable, indexer)
        let mut special_methods = JavaSpecialMethods::default();
        for method in &supported_methods {
            match &method.attrs.special_method {
                Some(SpecialMethod::Iterator) => {
                    // Iterator can return Nullable(OutType) or Infallible(OutType(Opaque { optional }))
                    // Use special_method_presence.iterator which has the cleaned-up type
                    if let Some(SuccessType::OutType(item_ty)) =
                        ty.special_method_presence.iterator.as_ref()
                    {
                        special_methods.iterator_type =
                            Some(self.gen_out_type_java_boxed(item_ty, type_name));
                    }
                }
                Some(SpecialMethod::Iterable) => {
                    // The return type is the concrete iterator class
                    if let ReturnType::Infallible(SuccessType::OutType(Type::Opaque(op))) =
                        &method.output
                    {
                        let iter_type_name =
                            self.formatter.fmt_type_name(op.tcx_id.into()).to_string();
                        special_methods.iterable_type = Some(iter_type_name);
                        // Resolve the iterator's yield type to get Iterable<ItemType>
                        let iter_def = self.tcx.resolve_opaque(op.tcx_id);
                        if let Some(SuccessType::OutType(item_ty)) =
                            iter_def.special_method_presence.iterator.as_ref()
                        {
                            special_methods.iterable_item_type =
                                Some(self.gen_out_type_java_boxed(item_ty, type_name));
                        }
                    }
                }
                Some(SpecialMethod::Indexer) => {
                    if let ReturnType::Nullable(SuccessType::OutType(ty)) = &method.output {
                        let item_type = self.gen_out_type_java_boxed(ty, type_name);
                        let index_type = method
                            .params
                            .first()
                            .map(|p| match &p.ty {
                                Type::Primitive(prim) => self
                                    .formatter
                                    .fmt_primitive_as_java(*prim)
                                    .to_string(),
                                _ => "long".to_string(),
                            })
                            .unwrap_or_else(|| "long".to_string());
                        special_methods.indexer_type = Some(JavaIndexerType {
                            index_type,
                            item_type,
                        });
                    }
                }
                _ => {}
            }
        }

        // Iterator/indexer internal methods use raw null instead of Optional,
        // so only count non-special nullable methods for uses_optional
        let non_special_methods: Vec<&Method> = supported_methods
            .iter()
            .filter(|m| {
                !matches!(
                    m.attrs.special_method,
                    Some(SpecialMethod::Iterator) | Some(SpecialMethod::Indexer)
                )
            })
            .copied()
            .collect();
        let uses_optional = self.methods_use_optional(&non_special_methods);

        let native_methods: Vec<JavaNativeMethodInfo> = supported_methods
            .iter()
            .map(|method| self.gen_native_method_info(method))
            .collect();

        let constructor_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter(|method| {
                method.param_self.is_none()
                    && matches!(
                        method.attrs.special_method,
                        Some(SpecialMethod::Constructor)
                    )
            })
            .map(|method| self.gen_method(method, None, type_name, is_error, None))
            .collect();

        let companion_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter(|method| {
                method.param_self.is_none()
                    && !matches!(
                        method.attrs.special_method,
                        Some(SpecialMethod::Constructor)
                    )
            })
            .map(|method| self.gen_method(method, None, type_name, false, None))
            .collect();

        let self_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter_map(|method| {
                method
                    .param_self
                    .as_ref()
                    .map(|self_param| (*method, &self_param.ty))
            })
            .map(|(method, self_type)| self.gen_method(method, Some(self_type), type_name, false, None))
            .collect();

        // Collect all callback declarations from all methods
        let all_methods_iter = constructor_methods
            .iter()
            .chain(companion_methods.iter())
            .chain(self_methods.iter());
        let mut all_cb_interfaces = Vec::new();
        let mut all_cb_runners = Vec::new();
        let mut all_cb_statics = Vec::new();
        for m in all_methods_iter {
            all_cb_interfaces.extend(m.callback_interfaces.iter().cloned());
            all_cb_runners.extend(m.callback_runners.iter().cloned());
            all_cb_statics.extend(m.callback_statics.iter().cloned());
        }
        let has_callbacks = !all_cb_interfaces.is_empty();

        #[derive(Template)]
        #[template(path = "java/Opaque.java.jinja", escape = "none")]
        struct ImplTemplate<'a> {
            domain: &'a str,
            lib_name: &'a str,
            dylib_name: &'a str,
            type_name: &'a str,
            docs: &'a str,
            dtor_abi_name: &'a str,
            is_error: bool,
            uses_optional: bool,
            has_callbacks: bool,
            native_methods: &'a [JavaNativeMethodInfo],
            constructor_methods: &'a [JavaMethodInfo],
            companion_methods: &'a [JavaMethodInfo],
            self_methods: &'a [JavaMethodInfo],
            special_methods: &'a JavaSpecialMethods,
            callback_interfaces: &'a [String],
            callback_runners: &'a [String],
            callback_statics: &'a [String],
        }

        let docs = self.formatter.fmt_javadoc_block(&ty.docs, "");

        (
            self.java_file_path(type_name),
            ImplTemplate {
                domain: self.domain,
                lib_name: self.lib_name,
                dylib_name: self.dylib_name,
                type_name,
                docs: &docs,
                dtor_abi_name: ty.dtor_abi_name.as_str(),
                is_error,
                uses_optional,
                has_callbacks,
                native_methods: &native_methods,
                constructor_methods: &constructor_methods,
                companion_methods: &companion_methods,
                self_methods: &self_methods,
                special_methods: &special_methods,
                callback_interfaces: &all_cb_interfaces,
                callback_runners: &all_cb_runners,
                callback_statics: &all_cb_statics,
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
        if let Some(layout) = self.type_to_ffi_layout(ty) {
            layouts.push(layout);
            return;
        }
        match ty {
            Type::Slice(Slice::Str(_, _)) | Type::Slice(Slice::Primitive(_, _)) => {
                // Slices are passed as { ADDRESS data, JAVA_LONG len }
                layouts.push("ValueLayout.ADDRESS".to_string());
                layouts.push("ValueLayout.JAVA_LONG".to_string());
            }
            Type::Callback(_) => {
                layouts.push("DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT".to_string());
            }
            Type::ImplTrait(trt) => {
                let trait_id = trt.id();
                let trait_name = self.formatter.fmt_trait_name(trait_id);
                layouts.push(format!("{trait_name}.TRAIT_STRUCT_LAYOUT"));
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
        match self.type_to_ffi_layout(ty) {
            some @ Some(_) => some,
            None => {
                self.errors
                    .push_error(format!("Unsupported return type in Java backend: {ty:?}"));
                None
            }
        }
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
                    SuccessType::OutType(ty) => self.field_size_align(ty),
                    SuccessType::Write => (0, 0), // Write is handled separately
                    SuccessType::Unit => (0, 0),
                    _ => (0, 0),
                };
                let err_size_align = match err {
                    Some(ty) => self.field_size_align(ty),
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
                    SuccessType::OutType(ty) => self.field_size_align(ty),
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
        is_error: bool,
        struct_fields: Option<&[JavaStructFieldInfo]>,
    ) -> JavaMethodInfo {
        self.gen_method_inner(method, self_type, owner_type_name, is_error, struct_fields, true)
    }

    /// Like gen_method, but with `honor_constructors = false` to suppress constructor syntax.
    fn gen_method_no_constructors(
        &self,
        method: &'cx Method,
        self_type: Option<&SelfType>,
        owner_type_name: &str,
    ) -> JavaMethodInfo {
        self.gen_method_inner(method, self_type, owner_type_name, false, None, false)
    }

    fn gen_method_inner(
        &self,
        method: &'cx Method,
        self_type: Option<&SelfType>,
        owner_type_name: &str,
        is_error: bool,
        struct_fields: Option<&[JavaStructFieldInfo]>,
        honor_constructors: bool,
    ) -> JavaMethodInfo {
        let _guard = self.errors.set_context_method(method.name.as_str().into());

        let is_constructor = honor_constructors
            && matches!(
                method.attrs.special_method,
                Some(SpecialMethod::Constructor)
            );
        let is_named_constructor = honor_constructors
            && matches!(
                method.attrs.special_method,
                Some(SpecialMethod::NamedConstructor(_))
            );
        let is_iterator = matches!(method.attrs.special_method, Some(SpecialMethod::Iterator));
        let is_iterable = matches!(method.attrs.special_method, Some(SpecialMethod::Iterable));
        let is_indexer = matches!(method.attrs.special_method, Some(SpecialMethod::Indexer));

        let method_name: Cow<'_, str> = if is_constructor {
            // Constructors use the class name, no method name needed
            owner_type_name.into()
        } else if is_named_constructor {
            if let Some(SpecialMethod::NamedConstructor(ref name)) = method.attrs.special_method {
                self.formatter.fmt_named_constructor_name(name, method)
            } else {
                self.formatter.fmt_method_name(method)
            }
        } else if is_iterator {
            "nextInternal".into()
        } else if is_indexer {
            "getInternal".into()
        } else if is_iterable {
            "iterator".into()
        } else {
            self.formatter.fmt_method_name(method)
        };

        let abi_handle = method.abi_name.as_str().to_uppercase();

        // For iterator/indexer, use boxed nullable type instead of Optional<T>.
        // For optional opaque returns (Infallible(Opaque{optional})), strip the Optional wrapper.
        let return_type_java = if is_iterator || is_indexer {
            match &method.output {
                ReturnType::Nullable(SuccessType::OutType(ty)) => {
                    self.gen_out_type_java_boxed(ty, owner_type_name)
                }
                ReturnType::Infallible(SuccessType::OutType(Type::Opaque(op))) => {
                    // Strip optional: just the type name
                    let type_id: TypeId = op.tcx_id.into();
                    self.formatter.fmt_type_name(type_id).to_string()
                }
                ReturnType::Infallible(SuccessType::OutType(ty)) => {
                    self.gen_out_type_java_boxed(ty, owner_type_name)
                }
                _ => self.gen_return_type_java(&method.output, owner_type_name),
            }
        } else {
            self.gen_return_type_java(&method.output, owner_type_name)
        };
        let is_static = self_type.is_none();

        // Build parameter list for Java signature
        let mut java_params = Vec::new();
        // Build invoke arguments (what gets passed to invokeExact)
        let mut invoke_args = Vec::new();
        // Track string params that need Arena allocation, with their encoding
        let mut string_params: Vec<(String, StringEncoding)> = Vec::new();
        // Track primitive slice params that need Arena allocation
        let mut slice_params: Vec<(String, hir::PrimitiveType)> = Vec::new();
        // Track nullable opaque params that need local variable setup
        let mut nullable_setup_lines: Vec<String> = Vec::new();
        // Track callback params
        let mut callback_infos: Vec<JavaCallbackInfo> = Vec::new();
        // Track trait params (param_name, trait_name)
        let mut trait_setup_params: Vec<(String, String)> = Vec::new();
        // Track if any struct params need arena for toNative
        let mut has_struct_param = false;

        let method_name_for_cb = self.formatter.fmt_method_name(method).to_string();

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
                &mut slice_params,
                &mut nullable_setup_lines,
                &mut callback_infos,
                &mut trait_setup_params,
                &method_name_for_cb,
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

        let has_callbacks = !callback_infos.is_empty();
        let has_traits = !trait_setup_params.is_empty();

        // Determine if we need an arena
        let needs_arena = !string_params.is_empty()
            || !slice_params.is_empty()
            || has_struct_param
            || returns_struct
            || is_fallible
            || is_nullable
            || has_callbacks
            || has_traits;

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
        let raw_nullable = is_iterator || is_indexer;
        let return_stmt = if is_fallible || is_nullable {
            self.gen_result_return_stmt(method, &invoke_call, &abi_handle, is_write_return, is_constructor, struct_fields, raw_nullable)
        } else if is_write_return {
            format!("{invoke_call};\n            return DiplomatLib.writeToString(write);")
        } else if is_constructor {
            // Constructor: assign fields instead of returning
            self.gen_constructor_assign_stmt(&method.output, &invoke_call, struct_fields)
        } else {
            match &method.output {
                ReturnType::Infallible(SuccessType::Unit) => format!("{invoke_call};"),
                ReturnType::Infallible(SuccessType::OutType(ty)) => {
                    if let Type::Opaque(op) = ty {
                        if op.is_optional() {
                            let type_name = self.fmt_type_name_str(ty);
                            if raw_nullable {
                                format!(
                                    "var resultAddr = (MemorySegment) {invoke_call};\n            return resultAddr.equals(MemorySegment.NULL) ? null : new {type_name}(resultAddr);"
                                )
                            } else {
                                format!(
                                    "var resultAddr = (MemorySegment) {invoke_call};\n            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new {type_name}(resultAddr));"
                                )
                            }
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

        // For error type constructors, prepend super() call
        let super_call = if is_constructor && is_error {
            format!("        super(\"{owner_type_name}\");\n")
        } else {
            String::new()
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
            for (sp, prim) in &slice_params {
                if matches!(prim, hir::PrimitiveType::Bool) {
                    // boolean[] not supported by allocateFrom, convert to byte[]
                    setup_lines.push_str(&format!(
                        "            byte[] {sp}Bytes = new byte[{sp}.length];\n\
                         \x20           for (int i = 0; i < {sp}.length; i++) {sp}Bytes[i] = {sp}[i] ? (byte) 1 : (byte) 0;\n\
                         \x20           var {sp}Seg = arena.allocateFrom(ValueLayout.JAVA_BYTE, {sp}Bytes);\n"
                    ));
                } else {
                    let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                    setup_lines.push_str(&format!(
                        "            var {sp}Seg = arena.allocateFrom({layout}, {sp});\n"
                    ));
                }
            }
            for cb_info in &callback_infos {
                setup_lines.push_str(&format!(
                    "            {}\n",
                    self.gen_callback_setup_code(cb_info)
                ));
            }
            for (tp_name, tp_trait_name) in &trait_setup_params {
                setup_lines.push_str(&format!(
                    "            var {tp_name}Native = {tp_trait_name}.createNative({tp_name}, arena);\n"
                ));
            }
            format!("{super_call}{write_setup}        try (var arena = Arena.ofConfined()) {{\n{setup_lines}{nullable_setup}            {return_stmt}\n        }} catch (RuntimeException ex) {{\n            throw ex;\n        }} catch (Throwable ex) {{\n            throw new RuntimeException(ex);\n        }}")
        } else {
            format!("{super_call}{write_setup}        try {{\n{nullable_setup}            {return_stmt}\n        }} catch (RuntimeException ex) {{\n            throw ex;\n        }} catch (Throwable ex) {{\n            throw new RuntimeException(ex);\n        }}")
        };

        // Build declaration
        let definition = if is_constructor {
            format!(
                "public {owner_type_name}({java_params_str}) {{\n{body}\n    }}"
            )
        } else if is_named_constructor {
            format!(
                "public static {return_type_java} {method_name}({java_params_str}) {{\n{body}\n    }}"
            )
        } else if is_iterator || is_indexer {
            format!(
                "private {return_type_java} {method_name}({java_params_str}) {{\n{body}\n    }}"
            )
        } else if is_iterable {
            format!(
                "@Override\n    public {return_type_java} {method_name}({java_params_str}) {{\n{body}\n    }}"
            )
        } else {
            let static_kw = if is_static { "static " } else { "" };
            format!(
                "public {static_kw}{return_type_java} {method_name}({java_params_str}) {{\n{body}\n    }}"
            )
        };

        // Generate callback class-level declarations
        let mut cb_interfaces = Vec::new();
        let mut cb_runners = Vec::new();
        let mut cb_statics = Vec::new();
        for (cb_info, param) in callback_infos.iter().zip(
            method
                .params
                .iter()
                .filter(|p| matches!(&p.ty, Type::Callback(_))),
        ) {
            if let Type::Callback(ref cb) = param.ty {
                let cb_params = cb.get_inputs().expect("callback must have inputs");
                let cb_output = cb.get_output_type().expect("callback must have output");
                cb_interfaces.push(self.gen_callback_interface(cb_info));
                cb_runners.push(self.gen_callback_runner(cb_info, cb_output, owner_type_name));
                cb_statics
                    .push(self.gen_callback_statics(cb_info, cb_params, cb_output, owner_type_name));
            }
        }

        let docs = if is_iterator || is_indexer || is_iterable {
            String::new()
        } else {
            self.formatter.fmt_javadoc_block(&method.docs, "    ")
        };

        JavaMethodInfo {
            docs,
            definition,
            callback_interfaces: cb_interfaces,
            callback_runners: cb_runners,
            callback_statics: cb_statics,
        }
    }

    /// Generate the assignment statement for a constructor (instead of return).
    fn gen_constructor_assign_stmt(
        &self,
        output: &ReturnType,
        invoke_call: &str,
        struct_fields: Option<&[JavaStructFieldInfo]>,
    ) -> String {
        match output {
            ReturnType::Infallible(SuccessType::OutType(ty)) => match ty {
                Type::Opaque(_) => {
                    format!("this.handle = (MemorySegment) {invoke_call};")
                }
                Type::Struct(_) => {
                    let mut lines = Vec::new();
                    lines.push(format!("var seg = (MemorySegment) {invoke_call};"));
                    if let Some(fields) = struct_fields {
                        for f in fields {
                            lines.push(format!("this.{} = {};", f.field_name, f.from_native_expr));
                        }
                    }
                    lines.join("\n            ")
                }
                _ => format!("{invoke_call};"),
            },
            ReturnType::Infallible(SuccessType::Unit) => format!("{invoke_call};"),
            _ => format!("{invoke_call};"),
        }
    }

    /// Generate the return statement for a fallible or nullable method.
    /// When `raw_nullable` is true, nullable returns use raw null instead of Optional.
    #[allow(clippy::too_many_arguments)]
    fn gen_result_return_stmt(
        &self,
        method: &Method,
        invoke_call: &str,
        _abi_handle: &str,
        is_write_return: bool,
        is_constructor: bool,
        struct_fields: Option<&[JavaStructFieldInfo]>,
        raw_nullable: bool,
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
                let ok_extract = self.gen_ok_extract(ok, is_write_return, is_constructor, struct_fields);
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
            ReturnType::Nullable(ok) if raw_nullable => {
                let ok_extract = self.gen_nullable_raw_extract(ok);
                lines.push("if (isOk) {".to_string());
                lines.push(format!("    {ok_extract}"));
                lines.push("} else {".to_string());
                lines.push("    return null;".to_string());
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
    fn gen_ok_extract(
        &self,
        ok: &SuccessType,
        is_write_return: bool,
        is_constructor: bool,
        struct_fields: Option<&[JavaStructFieldInfo]>,
    ) -> String {
        if is_write_return {
            return "return DiplomatLib.writeToString(write);".to_string();
        }
        if is_constructor {
            return self.gen_ok_extract_constructor(ok, struct_fields);
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

    /// Generate the ok-branch for a fallible constructor.
    fn gen_ok_extract_constructor(
        &self,
        ok: &SuccessType,
        struct_fields: Option<&[JavaStructFieldInfo]>,
    ) -> String {
        match ok {
            SuccessType::Unit => String::new(),
            SuccessType::OutType(ty) => match ty {
                Type::Opaque(_) => {
                    "this.handle = result.get(ValueLayout.ADDRESS, 0L);".to_string()
                }
                Type::Struct(_) => {
                    let type_name = self.fmt_type_name_str(ty);
                    let mut parts = Vec::new();
                    parts.push(format!(
                        "var seg = result.asSlice(0L, {type_name}.LAYOUT.byteSize());"
                    ));
                    if let Some(fields) = struct_fields {
                        for f in fields {
                            parts.push(format!("this.{} = {};", f.field_name, f.from_native_expr));
                        }
                    }
                    parts.join("\n                ")
                }
                _ => String::new(),
            },
            _ => String::new(),
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

    /// Generate the ok-branch extraction for a raw nullable return (returns value or null).
    fn gen_nullable_raw_extract(&self, ok: &SuccessType) -> String {
        match ok {
            SuccessType::OutType(ty) => {
                let extract = self.gen_result_value_extract(ty, "result");
                format!("return {extract};")
            }
            SuccessType::Write => {
                "return DiplomatLib.writeToString(write);".to_string()
            }
            _ => "return null;".to_string(),
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
                let type_name = self.fmt_type_name_str(ty);
                format!("new {type_name}({seg_name}.get(ValueLayout.ADDRESS, 0L))")
            }
            Type::Enum(_) => {
                let type_name = self.fmt_type_name_str(ty);
                format!("{type_name}.fromNative((int) {seg_name}.get(ValueLayout.JAVA_INT, 0L))")
            }
            Type::Struct(_) => {
                let type_name = self.fmt_type_name_str(ty);
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
                    let type_name = self.fmt_type_name_str(err_ty);
                    let type_id = err_ty.id().expect("enum must have id");
                    let resolved = self.formatter.tcx().resolve_type(type_id);
                    if resolved.attrs().custom_errors {
                        format!("throw new {type_name}Exception({type_name}.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L)));")
                    } else {
                        format!("throw new RuntimeException(\"{type_name} error: \" + {type_name}.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L)));")
                    }
                }
                Type::Opaque(_) => {
                    let type_name = self.fmt_type_name_str(err_ty);
                    format!(
                        "throw new {type_name}(result.get(ValueLayout.ADDRESS, 0L));"
                    )
                }
                Type::Struct(_) => {
                    let type_name = self.fmt_type_name_str(err_ty);
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
        slice_params: &mut Vec<(String, hir::PrimitiveType)>,
        nullable_setup_lines: &mut Vec<String>,
        callback_infos: &mut Vec<JavaCallbackInfo>,
        trait_setup_params: &mut Vec<(String, String)>, // (param_name, trait_name)
        method_name_for_cb: &str,
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
            Type::Slice(Slice::Primitive(_, prim)) => {
                let java_type = self.formatter.fmt_primitive_as_java(*prim);
                java_params.push(format!("{java_type}[] {param_name}"));
                slice_params.push((param_name.to_string(), *prim));
                invoke_args.push(format!("{param_name}Seg"));
                invoke_args.push(format!("(long) {param_name}.length"));
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
            Type::Callback(ref cb) => {
                let params = cb.get_inputs().expect("callback must have inputs");
                let output = cb.get_output_type().expect("callback must have output");
                let cb_info = self.gen_callback_info_from_parts(
                    params,
                    output,
                    param_name,
                    method_name_for_cb,
                );
                java_params.push(format!("{} {param_name}", cb_info.interface_name));
                let native_seg_name = format!("{param_name}Native");
                invoke_args.push(native_seg_name);
                callback_infos.push(cb_info);
            }
            Type::ImplTrait(trt) => {
                let trait_id = trt.id();
                let trait_name = self.formatter.fmt_trait_name(trait_id).to_string();
                java_params.push(format!("{trait_name} {param_name}"));
                let native_seg_name = format!("{param_name}Native");
                invoke_args.push(native_seg_name);
                trait_setup_params.push((param_name.to_string(), trait_name));
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
                let type_name = self.fmt_type_name_str(ty);
                if op.is_optional() {
                    format!("Optional<{type_name}>")
                } else {
                    type_name
                }
            }
            Type::Enum(_) | Type::Struct(_) => self.fmt_type_name_str(ty),
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
                let type_name = self.fmt_type_name_str(ty);
                format!("new {type_name}((MemorySegment) {invoke_call})")
            }
            Type::Enum(_) => {
                let type_name = self.fmt_type_name_str(ty);
                format!("{type_name}.fromNative((int) {invoke_call})")
            }
            Type::Struct(_) => {
                let type_name = self.fmt_type_name_str(ty);
                format!(
                    "{type_name}.fromNative((MemorySegment) {invoke_call})"
                )
            }
            _ => invoke_call.to_string(),
        }
    }

    /// Build a JavaCallbackInfo from callback parts (params + output).
    fn gen_callback_info_from_parts(
        &self,
        params: &[hir::CallbackParam],
        output: &ReturnType<InputOnly>,
        param_name: &str,
        method_name: &str,
    ) -> JavaCallbackInfo {
        let unique_name = format!("{method_name}_{param_name}");
        let interface_name = format!(
            "{}{}",
            method_name.to_upper_camel_case(),
            param_name.to_upper_camel_case()
        );

        let mut interface_params = Vec::new();
        let mut runner_native_params = Vec::new();
        let mut runner_arg_conversions = Vec::new();
        let mut param_layouts = Vec::new();

        // Runner always receives MemorySegment data as first arg
        runner_native_params.push("MemorySegment data".to_string());

        for (i, cp) in params.iter().enumerate() {
            let arg_name = format!("arg{i}");
            let info = self.callback_param_types(&cp.ty, &arg_name);
            interface_params.push(format!("{} {arg_name}", info.java_type));
            runner_native_params.push(format!("{} {arg_name}", info.native_type));
            param_layouts.push(info.layout);
            runner_arg_conversions.push(info.conversion);
        }

        let return_info = self.callback_return_type_input(output);
        let interface_return_type = return_info.java_type;
        let return_layout = return_info.layout;
        let returns_void = return_info.is_void;

        JavaCallbackInfo {
            unique_name,
            param_name: param_name.to_string(),
            interface_name,
            interface_params,
            interface_return_type,
            runner_native_params,
            runner_arg_conversions,
            param_layouts,
            return_layout,
            returns_void,
        }
    }

    /// Get Java type, native (FFI) type, layout, and conversion expression for a callback param.
    fn callback_param_types(
        &self,
        ty: &OutType,
        arg_name: &str,
    ) -> CallbackParamTypeInfo {
        match ty {
            Type::Primitive(prim) => {
                let java_type = self.formatter.fmt_primitive_as_java(*prim).to_string();
                let layout = self.formatter.fmt_primitive_as_ffi(*prim).to_string();
                CallbackParamTypeInfo {
                    java_type: java_type.clone(),
                    native_type: java_type,
                    layout,
                    conversion: arg_name.to_string(),
                }
            }
            Type::Enum(_) => {
                let type_name = self.fmt_type_name_str(ty);
                CallbackParamTypeInfo {
                    java_type: type_name.clone(),
                    native_type: "int".to_string(),
                    layout: "ValueLayout.JAVA_INT".to_string(),
                    conversion: format!("{type_name}.fromNative({arg_name})"),
                }
            }
            Type::Struct(_) => {
                let type_name = self.fmt_type_name_str(ty);
                CallbackParamTypeInfo {
                    java_type: type_name.clone(),
                    native_type: "MemorySegment".to_string(),
                    layout: format!("{type_name}.LAYOUT"),
                    conversion: format!("{type_name}.fromNative({arg_name})"),
                }
            }
            Type::Opaque(_) => {
                let type_name = self.fmt_type_name_str(ty);
                CallbackParamTypeInfo {
                    java_type: type_name.clone(),
                    native_type: "MemorySegment".to_string(),
                    layout: "ValueLayout.ADDRESS".to_string(),
                    conversion: format!("new {type_name}({arg_name})"),
                }
            }
            Type::Slice(slice) => {
                let (java_type, conversion) = match slice {
                    Slice::Str(_, encoding) => {
                        let charset = match encoding {
                            StringEncoding::UnvalidatedUtf8 | StringEncoding::Utf8 => {
                                "StandardCharsets.UTF_8"
                            }
                            StringEncoding::UnvalidatedUtf16 => "StandardCharsets.UTF_16LE",
                            _ => "StandardCharsets.UTF_8",
                        };
                        let byte_multiplier = match encoding {
                            StringEncoding::UnvalidatedUtf16 => " * 2",
                            _ => "",
                        };
                        (
                            "String".to_string(),
                            format!(
                                "new String(((MemorySegment) DiplomatLib.VH_SV_DATA.get({arg_name}, 0L))\
                                 .reinterpret((long) DiplomatLib.VH_SV_LEN.get({arg_name}, 0L){byte_multiplier})\
                                 .toArray(ValueLayout.JAVA_BYTE), {charset})"
                            ),
                        )
                    }
                    Slice::Primitive(_, prim) => {
                        let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                        let (elem_size, _) = self.formatter.primitive_size_align(*prim);
                        let java_arr_type =
                            format!("{}[]", self.formatter.fmt_primitive_as_java(*prim));
                        (
                            java_arr_type,
                            format!(
                                "((MemorySegment) DiplomatLib.VH_SV_DATA.get({arg_name}, 0L))\
                                 .reinterpret((long) DiplomatLib.VH_SV_LEN.get({arg_name}, 0L) * {elem_size}L)\
                                 .toArray({layout})"
                            ),
                        )
                    }
                    _ => ("Object".to_string(), arg_name.to_string()),
                };
                CallbackParamTypeInfo {
                    java_type,
                    native_type: "MemorySegment".to_string(),
                    layout: "DiplomatLib.DIPLOMAT_STRING_VIEW".to_string(),
                    conversion,
                }
            }
            _ => CallbackParamTypeInfo {
                java_type: "Object".to_string(),
                native_type: "MemorySegment".to_string(),
                layout: "ValueLayout.ADDRESS".to_string(),
                conversion: arg_name.to_string(),
            },
        }
    }

    /// Get Java return type, layout string, and whether it's void for a callback's output (InputOnly position).
    fn callback_return_type_input(
        &self,
        output: &ReturnType<InputOnly>,
    ) -> ReturnTypeInfo {
        match output {
            ReturnType::Infallible(success) => match success {
                SuccessType::Unit => ReturnTypeInfo {
                    java_type: "void".to_string(),
                    layout: None,
                    is_void: true,
                },
                SuccessType::OutType(ty) => self.type_to_return_info(ty),
                _ => ReturnTypeInfo {
                    java_type: "void".to_string(),
                    layout: None,
                    is_void: true,
                },
            },
            _ => ReturnTypeInfo {
                java_type: "void".to_string(),
                layout: None,
                is_void: true,
            },
        }
    }

    /// Helper: get return type info for a type used as callback/trait return.
    fn type_to_return_info<P: hir::TyPosition>(&self, ty: &Type<P>) -> ReturnTypeInfo {
        match ty {
            Type::Primitive(prim) => {
                let java_type = self.formatter.fmt_primitive_as_java(*prim).to_string();
                let layout = self.formatter.fmt_primitive_as_ffi(*prim).to_string();
                ReturnTypeInfo {
                    java_type,
                    layout: Some(layout),
                    is_void: false,
                }
            }
            Type::Enum(_) => ReturnTypeInfo {
                java_type: self.fmt_type_name_str(ty),
                layout: Some("ValueLayout.JAVA_INT".to_string()),
                is_void: false,
            },
            Type::Struct(_) => {
                let type_name = self.fmt_type_name_str(ty);
                ReturnTypeInfo {
                    java_type: type_name.clone(),
                    layout: Some(format!("{type_name}.LAYOUT")),
                    is_void: false,
                }
            }
            _ => ReturnTypeInfo {
                java_type: "Object".to_string(),
                layout: Some("ValueLayout.ADDRESS".to_string()),
                is_void: false,
            },
        }
    }

    /// Generate the native return expression for a callback runner (converting Java -> native).
    fn callback_return_conversion_input(
        &self,
        output: &ReturnType<InputOnly>,
        expr: &str,
    ) -> String {
        match output {
            ReturnType::Infallible(SuccessType::OutType(ty)) => match ty {
                Type::Enum(_) => format!("{expr}.toNative()"),
                Type::Struct(_) => format!("{expr}.toNative(Arena.global())"),
                _ => expr.to_string(),
            },
            _ => expr.to_string(),
        }
    }

    /// Generate the @FunctionalInterface declaration for a callback.
    fn gen_callback_interface(&self, info: &JavaCallbackInfo) -> String {
        let params_str = info.interface_params.join(", ");
        format!(
            "    @FunctionalInterface\n    public interface {} {{\n        {} invoke({});\n    }}",
            info.interface_name, info.interface_return_type, params_str
        )
    }

    /// Generate the static runner method for a callback.
    fn gen_callback_runner(
        &self,
        info: &JavaCallbackInfo,
        cb_output: &ReturnType<InputOnly>,
        _owner_type_name: &str,
    ) -> String {
        let runner_params_str = info.runner_native_params.join(", ");
        let native_return_type = if info.returns_void {
            "void"
        } else {
            self.native_return_type_str(cb_output)
        };

        let arg_exprs: Vec<String> = info.runner_arg_conversions.clone();
        let invoke_args = arg_exprs.join(", ");

        let invoke_expr = format!("cb.invoke({invoke_args})");
        let body = if info.returns_void {
            format!("        {invoke_expr};")
        } else {
            let converted = self.callback_return_conversion_input(cb_output, &invoke_expr);
            format!("        return {converted};")
        };

        format!(
            "    private static {native_return_type} runCallback_{unique_name}({runner_params_str}) {{\n\
             \x20       @SuppressWarnings(\"unchecked\")\n\
             \x20       {iface} cb = DiplomatLib.getCallback(data.address(), {iface}.class);\n\
             {body}\n\
             \x20   }}",
            unique_name = info.unique_name,
            iface = info.interface_name,
        )
    }

    /// Generate the static MethodHandle + upcall stub declarations for a callback.
    fn gen_callback_statics(
        &self,
        info: &JavaCallbackInfo,
        cb_params: &[hir::CallbackParam],
        cb_output: &ReturnType<InputOnly>,
        owner_type_name: &str,
    ) -> String {
        let runner_name = format!("runCallback_{}", info.unique_name);
        let mh_name = format!("MH_RUN_{}", info.unique_name);
        let upcall_name = format!("UPCALL_{}", info.unique_name);

        // Build MethodType parameters (MemorySegment for data, then native param types)
        let mut mt_params = vec!["MemorySegment.class".to_string()];
        for cp in cb_params.iter() {
            mt_params.push(self.type_to_method_type_class(&cp.ty));
        }
        let mt_params_str = mt_params.join(", ");

        let mt_return = if info.returns_void {
            "void.class".to_string()
        } else {
            self.native_return_method_type_class(cb_output)
        };

        // Build FunctionDescriptor
        let mut fd_params = vec!["ValueLayout.ADDRESS".to_string()]; // data
        fd_params.extend(info.param_layouts.iter().cloned());
        let fd_params_str = fd_params.join(", ");
        let fd = if let Some(ref ret) = info.return_layout {
            format!("FunctionDescriptor.of({ret}, {fd_params_str})")
        } else {
            format!("FunctionDescriptor.ofVoid({fd_params_str})")
        };

        format!(
            "    private static final MethodHandle {mh_name};\n\
             \x20   private static final MemorySegment {upcall_name};\n\
             \x20   static {{\n\
             \x20       try {{\n\
             \x20           {mh_name} = MethodHandles.lookup().findStatic(\n\
             \x20               {owner_type_name}.class, \"{runner_name}\",\n\
             \x20               MethodType.methodType({mt_return}, {mt_params_str}));\n\
             \x20           {upcall_name} = DiplomatLib.LINKER_SHARED.upcallStub(\n\
             \x20               {mh_name},\n\
             \x20               {fd},\n\
             \x20               Arena.global());\n\
             \x20       }} catch (ReflectiveOperationException ex) {{\n\
             \x20           throw new ExceptionInInitializerError(ex);\n\
             \x20       }}\n\
             \x20   }}"
        )
    }

    /// Get the native return type string (for runner method signature) from a callback output.
    fn native_return_type_str(&self, output: &ReturnType<InputOnly>) -> &'static str {
        match output {
            ReturnType::Infallible(SuccessType::OutType(ty)) => match ty {
                Type::Primitive(prim) => self.formatter.fmt_primitive_as_java(*prim),
                Type::Enum(_) => "int",
                Type::Struct(_) => "MemorySegment",
                _ => "Object",
            },
            _ => "void",
        }
    }

    /// Get MethodType class literal for a callback return type.
    fn native_return_method_type_class(&self, output: &ReturnType<InputOnly>) -> String {
        match output {
            ReturnType::Infallible(SuccessType::OutType(ty)) => {
                self.type_to_method_type_class(ty)
            }
            _ => "void.class".to_string(),
        }
    }

    /// Generate the setup code in a method body for a single callback parameter.
    fn gen_callback_setup_code(&self, info: &JavaCallbackInfo) -> String {
        let id_var = format!("{}Id", info.param_name);
        let native_var = format!("{}Native", info.param_name);
        let upcall_name = format!("UPCALL_{}", info.unique_name);

        format!(
            "long {id_var} = DiplomatLib.registerCallback({param});\n\
             \x20           var {native_var} = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);\n\
             \x20           DiplomatLib.VH_CB_DATA.set({native_var}, 0L, MemorySegment.ofAddress({id_var}));\n\
             \x20           DiplomatLib.VH_CB_RUN.set({native_var}, 0L, {upcall_name});\n\
             \x20           DiplomatLib.VH_CB_DESTRUCTOR.set({native_var}, 0L, DiplomatLib.DESTRUCTOR_STUB);",
            param = info.param_name,
        )
    }

    /// Generate trait definition file.
    fn gen_trait_def(
        &self,
        trt: &'cx hir::TraitDef,
        trait_name: &str,
    ) -> (String, String) {
        let trait_methods: Vec<_> = trt
            .methods
            .iter()
            .filter(|m| {
                if let Some(m_attrs) = &m.attrs {
                    !m_attrs.disable
                } else {
                    true
                }
            })
            .collect();

        // Build interface method signatures
        let mut interface_methods = Vec::new();
        for method in &trait_methods {
            let method_name = self.formatter.fmt_trait_method_name(method);
            let mut params = Vec::new();
            for (i, cp) in method.params.iter().enumerate() {
                let arg_name = cp
                    .name
                    .as_ref()
                    .map(|n| n.as_str().to_lower_camel_case())
                    .unwrap_or_else(|| format!("arg{i}"));
                let java_type = self.type_to_java_name(&cp.ty);
                params.push(format!("{java_type} {arg_name}"));
            }
            let return_type = self.trait_method_return_type(&method.output);
            let params_str = params.join(", ");
            let method_docs = method
                .docs
                .as_ref()
                .map(|d| self.formatter.fmt_docs(d))
                .unwrap_or_default();
            let mut entry = String::new();
            if !method_docs.is_empty() {
                entry.push_str("    /**\n");
                for line in method_docs.lines() {
                    let trimmed = line.trim_end();
                    if trimmed.is_empty() {
                        entry.push_str("     *\n");
                    } else {
                        entry.push_str(&format!("     * {trimmed}\n"));
                    }
                }
                entry.push_str("     */\n");
            }
            entry.push_str(&format!("    {return_type} {method_name}({params_str});"));
            interface_methods.push(entry);
        }

        // Build vtable layout fields: destructor, size, alignment, then per-method callback
        let mut vtable_fields = Vec::new();
        vtable_fields.push("ValueLayout.ADDRESS.withName(\"destructor\")".to_string());
        vtable_fields.push("ValueLayout.JAVA_LONG.withName(\"size\")".to_string());
        vtable_fields.push("ValueLayout.JAVA_LONG.withName(\"alignment\")".to_string());
        for method in &trait_methods {
            let method_name = self.formatter.fmt_trait_method_name(method);
            vtable_fields.push(format!(
                "ValueLayout.ADDRESS.withName(\"run_{method_name}_callback\")"
            ));
        }

        // Build runner methods, MH/upcall statics for each method
        let mut runners = Vec::new();
        let mut statics = Vec::new();
        for method in &trait_methods {
            let method_name = self.formatter.fmt_trait_method_name(method).to_string();
            let (runner, static_decl) =
                self.gen_trait_method_upcall(method, &method_name, trait_name);
            runners.push(runner);
            statics.push(static_decl);
        }

        // Build createNative method
        let mut create_native_lines = Vec::new();
        create_native_lines.push("    static MemorySegment createNative(Object impl_, Arena arena) {".to_string());
        create_native_lines.push("        long id = DiplomatLib.registerCallback(impl_);".to_string());
        create_native_lines.push("        var seg = arena.allocate(TRAIT_STRUCT_LAYOUT);".to_string());
        create_native_lines.push("        VH_DATA.set(seg, 0L, MemorySegment.ofAddress(id));".to_string());
        create_native_lines.push("        VH_DESTRUCTOR.set(seg, 0L, DiplomatLib.DESTRUCTOR_STUB);".to_string());
        create_native_lines.push("        VH_SIZE.set(seg, 0L, 0L);".to_string());
        create_native_lines.push("        VH_ALIGNMENT.set(seg, 0L, 0L);".to_string());
        for method in &trait_methods {
            let method_name = self.formatter.fmt_trait_method_name(method);
            let upcall_name = format!("Statics.UPCALL_{method_name}");
            let vh_name = format!("VH_RUN_{}", method_name.to_shouty_snake_case());
            create_native_lines.push(format!("        {vh_name}.set(seg, 0L, {upcall_name});"));
        }
        create_native_lines.push("        return seg;".to_string());
        create_native_lines.push("    }".to_string());

        // Build VarHandles for data and vtable fields
        let mut var_handles = Vec::new();
        var_handles.push(
            "    VarHandle VH_DATA = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"data\"));".to_string()
        );
        var_handles.push(
            "    VarHandle VH_DESTRUCTOR = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"vtable\"), MemoryLayout.PathElement.groupElement(\"destructor\"));".to_string()
        );
        var_handles.push(
            "    VarHandle VH_SIZE = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"vtable\"), MemoryLayout.PathElement.groupElement(\"size\"));".to_string()
        );
        var_handles.push(
            "    VarHandle VH_ALIGNMENT = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"vtable\"), MemoryLayout.PathElement.groupElement(\"alignment\"));".to_string()
        );
        for method in &trait_methods {
            let method_name = self.formatter.fmt_trait_method_name(method);
            let vh_name = format!("VH_RUN_{}", method_name.to_shouty_snake_case());
            var_handles.push(format!(
                "    VarHandle {vh_name} = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"vtable\"), MemoryLayout.PathElement.groupElement(\"run_{method_name}_callback\"));"
            ));
        }

        // Build Statics inner class body
        let mut statics_class_body = Vec::new();
        statics_class_body.push("        private Statics() {}".to_string());
        // Add runners as static methods in the Statics class
        for runner in &runners {
            statics_class_body.push(format!("    {}", runner.replace("\n    ", "\n        ")));
        }
        // Add MH + upcall static fields + initialization
        for method in &trait_methods {
            let method_name = self.formatter.fmt_trait_method_name(method).to_string();
            let mh_name = format!("MH_{}", method_name.to_shouty_snake_case());
            let upcall_name = format!("UPCALL_{method_name}");

            let return_info = self.callback_return_type_input(&method.output);
            let return_layout = return_info.layout;
            let returns_void = return_info.is_void;

            let mut mt_params = vec!["MemorySegment.class".to_string()];
            let mut fd_params = vec!["ValueLayout.ADDRESS".to_string()];
            for cp in method.params.iter() {
                mt_params.push(self.type_to_method_type_class(&cp.ty));
                let info = self.callback_param_types(&cp.ty, "x");
                fd_params.push(info.layout);
            }
            let mt_return = if returns_void {
                "void.class".to_string()
            } else {
                self.native_return_method_type_class(&method.output)
            };
            let mt_params_str = mt_params.join(", ");
            let fd_params_str = fd_params.join(", ");
            let fd = if let Some(ref ret) = return_layout {
                format!("FunctionDescriptor.of({ret}, {fd_params_str})")
            } else {
                format!("FunctionDescriptor.ofVoid({fd_params_str})")
            };

            statics_class_body.push(format!("        static final MethodHandle {mh_name};"));
            statics_class_body.push(format!("        static final MemorySegment {upcall_name};"));
            statics_class_body.push(format!(
                "        static {{\n\
                 \x20           try {{\n\
                 \x20               {mh_name} = MethodHandles.lookup().findStatic(\n\
                 \x20                   Statics.class, \"traitRunner_{method_name}\",\n\
                 \x20                   MethodType.methodType({mt_return}, {mt_params_str}));\n\
                 \x20               {upcall_name} = DiplomatLib.LINKER_SHARED.upcallStub(\n\
                 \x20                   {mh_name},\n\
                 \x20                   {fd},\n\
                 \x20                   Arena.global());\n\
                 \x20           }} catch (ReflectiveOperationException ex) {{\n\
                 \x20               throw new ExceptionInInitializerError(ex);\n\
                 \x20           }}\n\
                 \x20       }}"
            ));
        }

        // Build complete file content
        let domain = self.domain;
        let lib_name = self.lib_name;
        let mut body = String::new();
        body.push_str(&format!("package {domain}.{lib_name};\n\n"));
        body.push_str("import java.lang.foreign.*;\n");
        body.push_str("import java.lang.invoke.MethodHandle;\n");
        body.push_str("import java.lang.invoke.MethodHandles;\n");
        body.push_str("import java.lang.invoke.MethodType;\n");
        body.push_str("import java.lang.invoke.VarHandle;\n\n");
        let docs = self.formatter.fmt_docs(&trt.docs);
        if !docs.is_empty() {
            body.push_str("/**\n");
            for line in docs.lines() {
                let trimmed = line.trim_end();
                if trimmed.is_empty() {
                    body.push_str(" *\n");
                } else {
                    body.push_str(&format!(" * {trimmed}\n"));
                }
            }
            body.push_str(" */\n");
        }
        body.push_str(&format!("public interface {trait_name} {{\n"));
        for m in &interface_methods {
            body.push_str(&format!("{m}\n"));
        }
        body.push('\n');
        // VTABLE_LAYOUT
        body.push_str("    StructLayout VTABLE_LAYOUT = MemoryLayout.structLayout(\n        ");
        body.push_str(&vtable_fields.join(",\n        "));
        body.push_str("\n    );\n");
        // TRAIT_STRUCT_LAYOUT
        body.push_str("    StructLayout TRAIT_STRUCT_LAYOUT = MemoryLayout.structLayout(\n");
        body.push_str("        ValueLayout.ADDRESS.withName(\"data\"),\n");
        body.push_str("        VTABLE_LAYOUT.withName(\"vtable\")\n");
        body.push_str("    );\n");
        // VarHandles
        for vh in &var_handles {
            body.push_str(&format!("{vh}\n"));
        }
        body.push('\n');
        // Statics inner class
        body.push_str("    final class Statics {\n");
        for line in &statics_class_body {
            body.push_str(&format!("{line}\n"));
        }
        body.push_str("    }\n\n");
        // createNative
        for line in &create_native_lines {
            body.push_str(&format!("{line}\n"));
        }
        body.push_str("}\n");

        (
            self.java_file_path(trait_name),
            body,
        )
    }

    /// Generate the runner method and static MH+upcall for a single trait method.
    fn gen_trait_method_upcall(
        &self,
        method: &Callback,
        method_name: &str,
        trait_name: &str,
    ) -> (String, String) {
        let return_info = self.callback_return_type_input(&method.output);
        let returns_void = return_info.is_void;

        // Build runner params
        let mut runner_params = vec!["MemorySegment data".to_string()];
        let mut arg_conversions = Vec::new();
        let mut native_param_layouts = Vec::new();
        let mut mt_params = vec!["MemorySegment.class".to_string()];

        for (i, cp) in method.params.iter().enumerate() {
            let arg_name = cp
                .name
                .as_ref()
                .map(|n| n.as_str().to_lower_camel_case())
                .unwrap_or_else(|| format!("arg{i}"));
            let info = self.callback_param_types(&cp.ty, &arg_name);
            runner_params.push(format!("{} {arg_name}", info.native_type));
            arg_conversions.push(info.conversion);
            native_param_layouts.push(info.layout);
            mt_params.push(self.type_to_method_type_class(&cp.ty));
        }

        let native_return_type = if returns_void {
            "void"
        } else {
            self.native_return_type_str(&method.output)
        };

        let runner_params_str = runner_params.join(", ");
        let invoke_args = arg_conversions.join(", ");
        let invoke_expr = format!("impl_.{method_name}({invoke_args})");

        let body = if returns_void {
            format!("        {invoke_expr};")
        } else {
            let converted =
                self.callback_return_conversion_input(&method.output, &invoke_expr);
            format!("        return {converted};")
        };

        let runner = format!(
            "    private static {native_return_type} traitRunner_{method_name}({runner_params_str}) {{\n\
             \x20       {trait_name} impl_ = DiplomatLib.getCallback(data.address(), {trait_name}.class);\n\
             {body}\n\
             \x20   }}"
        );

        // Reference to Statics inner class fields (MH + upcall are initialized there)
        let mh_name = format!("MH_{}", method_name.to_shouty_snake_case());
        let upcall_name = format!("UPCALL_{method_name}");

        let combined_static = format!(
            "    MethodHandle {mh_name} = Statics.{mh_name};\n\
             \x20   MemorySegment {upcall_name} = Statics.{upcall_name};"
        );

        (runner, combined_static)
    }

    /// Get the Java return type for a trait method.
    fn trait_method_return_type(&self, output: &ReturnType<InputOnly>) -> String {
        match output {
            ReturnType::Infallible(success) => match success {
                SuccessType::Unit => "void".to_string(),
                SuccessType::OutType(ty) => self.type_to_java_name(ty),
                _ => "void".to_string(),
            },
            _ => "void".to_string(),
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
                docs: self.formatter.fmt_javadoc_block(&v.docs, "    "),
                variant_name: self.formatter.fmt_enum_variant_name(v).to_string(),
                discriminant: v.discriminant,
            })
            .collect();

        let supported_methods: Vec<&Method> = ty
            .methods
            .iter()
            .filter(|m| !m.attrs.disable && self.is_method_supported(m))
            .collect();

        let uses_optional = self.methods_use_optional(&supported_methods);

        let native_methods: Vec<JavaNativeMethodInfo> = supported_methods
            .iter()
            .map(|method| self.gen_native_method_info(method))
            .collect();

        let companion_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter(|method| method.param_self.is_none())
            .map(|method| self.gen_method_no_constructors(method, None, type_name))
            .collect();

        let self_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter_map(|method| {
                method
                    .param_self
                    .as_ref()
                    .map(|self_param| (*method, &self_param.ty))
            })
            .map(|(method, self_type)| self.gen_method_no_constructors(method, Some(self_type), type_name))
            .collect();

        #[derive(Template)]
        #[template(path = "java/Enum.java.jinja", escape = "none")]
        struct EnumTemplate<'a> {
            domain: &'a str,
            lib_name: &'a str,
            dylib_name: &'a str,
            type_name: &'a str,
            docs: &'a str,
            is_error: bool,
            uses_optional: bool,
            variants: &'a [JavaEnumVariantInfo],
            native_methods: &'a [JavaNativeMethodInfo],
            companion_methods: &'a [JavaMethodInfo],
            self_methods: &'a [JavaMethodInfo],
        }

        let docs = self.formatter.fmt_javadoc_block(&ty.docs, "");

        (
            self.java_file_path(type_name),
            EnumTemplate {
                domain: self.domain,
                lib_name: self.lib_name,
                dylib_name: self.dylib_name,
                type_name,
                docs: &docs,
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

        // Compute VarHandle and offset constant declarations
        let (var_handles, offset_consts) = self.compute_var_handles(&ty.fields);

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

        let uses_optional = self.methods_use_optional(&supported_methods);

        let native_methods: Vec<JavaNativeMethodInfo> = supported_methods
            .iter()
            .map(|method| self.gen_native_method_info(method))
            .collect();

        let constructor_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter(|method| {
                method.param_self.is_none()
                    && matches!(
                        method.attrs.special_method,
                        Some(SpecialMethod::Constructor)
                    )
            })
            .map(|method| self.gen_method(method, None, type_name, is_error, Some(&fields)))
            .collect();

        let has_zero_arg_constructor = supported_methods.iter().any(|method| {
            method.param_self.is_none()
                && matches!(
                    method.attrs.special_method,
                    Some(SpecialMethod::Constructor)
                )
                && method.params.is_empty()
        });

        let companion_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter(|method| {
                method.param_self.is_none()
                    && !matches!(
                        method.attrs.special_method,
                        Some(SpecialMethod::Constructor)
                    )
            })
            .map(|method| self.gen_method(method, None, type_name, false, None))
            .collect();

        let self_methods: Vec<JavaMethodInfo> = supported_methods
            .iter()
            .filter_map(|method| {
                method
                    .param_self
                    .as_ref()
                    .map(|self_param| (*method, &self_param.ty))
            })
            .map(|(method, self_type)| self.gen_method(method, Some(self_type), type_name, false, None))
            .collect();

        let has_constructors = !constructor_methods.is_empty();

        // Collect callback declarations from all methods
        let all_methods_iter = constructor_methods
            .iter()
            .chain(companion_methods.iter())
            .chain(self_methods.iter());
        let mut all_cb_interfaces = Vec::new();
        let mut all_cb_runners = Vec::new();
        let mut all_cb_statics = Vec::new();
        for m in all_methods_iter {
            all_cb_interfaces.extend(m.callback_interfaces.iter().cloned());
            all_cb_runners.extend(m.callback_runners.iter().cloned());
            all_cb_statics.extend(m.callback_statics.iter().cloned());
        }
        let has_callbacks = !all_cb_interfaces.is_empty();

        #[derive(Template)]
        #[template(path = "java/Struct.java.jinja", escape = "none")]
        struct StructTemplate<'a> {
            domain: &'a str,
            lib_name: &'a str,
            dylib_name: &'a str,
            type_name: &'a str,
            docs: &'a str,
            is_error: bool,
            is_out_struct: bool,
            uses_optional: bool,
            has_zero_arg_constructor: bool,
            has_constructors: bool,
            has_callbacks: bool,
            layout_members: &'a str,
            var_handles: &'a [JavaVarHandleInfo],
            offset_consts: &'a [JavaOffsetConstInfo],
            fields: &'a [JavaStructFieldInfo],
            native_methods: &'a [JavaNativeMethodInfo],
            constructor_methods: &'a [JavaMethodInfo],
            companion_methods: &'a [JavaMethodInfo],
            self_methods: &'a [JavaMethodInfo],
            callback_interfaces: &'a [String],
            callback_runners: &'a [String],
            callback_statics: &'a [String],
        }

        let docs = self.formatter.fmt_javadoc_block(&ty.docs, "");

        (
            self.java_file_path(type_name),
            StructTemplate {
                domain: self.domain,
                lib_name: self.lib_name,
                dylib_name: self.dylib_name,
                type_name,
                docs: &docs,
                is_error,
                is_out_struct,
                uses_optional,
                has_zero_arg_constructor,
                has_constructors,
                has_callbacks,
                layout_members: &layout_members,
                var_handles: &var_handles,
                offset_consts: &offset_consts,
                fields: &fields,
                native_methods: &native_methods,
                constructor_methods: &constructor_methods,
                companion_methods: &companion_methods,
                self_methods: &self_methods,
                callback_interfaces: &all_cb_interfaces,
                callback_runners: &all_cb_runners,
                callback_statics: &all_cb_statics,
            }
            .render()
            .expect("failed to render struct type"),
        )
    }

    /// Compute VarHandle and offset constant declarations for a struct's fields.
    fn compute_var_handles<P: hir::TyPosition>(
        &self,
        fields: &[StructField<P>],
    ) -> (Vec<JavaVarHandleInfo>, Vec<JavaOffsetConstInfo>) {
        let mut var_handles = Vec::new();
        let mut offset_consts = Vec::new();

        for field in fields.iter() {
            let field_name = self.formatter.fmt_field_name(field.name.as_str());
            let shouty = field_name.to_shouty_snake_case();

            match &field.ty {
                // Primitives, booleans, enums, opaques: single VarHandle
                Type::Primitive(_) | Type::Opaque(_) | Type::Enum(_) => {
                    var_handles.push(JavaVarHandleInfo {
                        handle_name: format!("VH_{shouty}"),
                        declaration: format!(
                            "LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"{field_name}\"))"
                        ),
                    });
                }
                // Nested struct: offset constant (accessed via asSlice)
                Type::Struct(_) => {
                    offset_consts.push(JavaOffsetConstInfo {
                        const_name: format!("OFFSET_{shouty}"),
                        declaration: format!(
                            "LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(\"{field_name}\"))"
                        ),
                    });
                }
                // Slice: two VarHandles for data and len sub-fields
                Type::Slice(_) => {
                    var_handles.push(JavaVarHandleInfo {
                        handle_name: format!("VH_{shouty}_DATA"),
                        declaration: format!(
                            "LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"{field_name}\"), MemoryLayout.PathElement.groupElement(\"data\"))"
                        ),
                    });
                    var_handles.push(JavaVarHandleInfo {
                        handle_name: format!("VH_{shouty}_LEN"),
                        declaration: format!(
                            "LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"{field_name}\"), MemoryLayout.PathElement.groupElement(\"len\"))"
                        ),
                    });
                }
                // DiplomatOption: VarHandle(s) for value + VarHandle for is_ok
                Type::DiplomatOption(inner) => {
                    match inner.as_ref() {
                        // For nested struct options: offset for value, VarHandle for is_ok
                        Type::Struct(_) => {
                            offset_consts.push(JavaOffsetConstInfo {
                                const_name: format!("OFFSET_{shouty}"),
                                declaration: format!(
                                    "LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(\"{field_name}\"))"
                                ),
                            });
                        }
                        // For slice options: VarHandles for data/len sub-fields
                        Type::Slice(_) => {
                            var_handles.push(JavaVarHandleInfo {
                                handle_name: format!("VH_{shouty}_DATA"),
                                declaration: format!(
                                    "LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"{field_name}\"), MemoryLayout.PathElement.groupElement(\"value\"), MemoryLayout.PathElement.groupElement(\"data\"))"
                                ),
                            });
                            var_handles.push(JavaVarHandleInfo {
                                handle_name: format!("VH_{shouty}_LEN"),
                                declaration: format!(
                                    "LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"{field_name}\"), MemoryLayout.PathElement.groupElement(\"value\"), MemoryLayout.PathElement.groupElement(\"len\"))"
                                ),
                            });
                        }
                        // For primitives/enums/opaques: VarHandle for value
                        _ => {
                            var_handles.push(JavaVarHandleInfo {
                                handle_name: format!("VH_{shouty}_VALUE"),
                                declaration: format!(
                                    "LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"{field_name}\"), MemoryLayout.PathElement.groupElement(\"value\"))"
                                ),
                            });
                        }
                    }
                    var_handles.push(JavaVarHandleInfo {
                        handle_name: format!("VH_{shouty}_IS_OK"),
                        declaration: format!(
                            "LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"{field_name}\"), MemoryLayout.PathElement.groupElement(\"is_ok\"))"
                        ),
                    });
                }
                _ => {}
            }
        }

        (var_handles, offset_consts)
    }

    /// Compute Java field info for each struct field.
    fn compute_struct_fields<P: hir::TyPosition>(
        &self,
        fields: &[StructField<P>],
        _type_name: &str,
    ) -> Vec<JavaStructFieldInfo> {
        fields
            .iter()
            .map(|field| {
                let field_name = self.formatter.fmt_field_name(field.name.as_str()).to_string();
                let java_type = self.field_java_type(&field.ty);
                let shouty = field_name.to_shouty_snake_case();
                let from_native_expr = self.field_from_native(&field.ty, &field_name, &shouty);
                let to_native_stmt = self.field_to_native(&field.ty, &field_name, &shouty);

                JavaStructFieldInfo {
                    docs: self.formatter.fmt_javadoc_block(&field.docs, "    "),
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
            let (f_size, f_align) = self.field_size_align(&field.ty);
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


    fn field_layout_element<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
        if let Some(layout) = self.type_to_ffi_layout(ty) {
            return layout;
        }
        match ty {
            Type::Slice(_) => "DiplomatLib.DIPLOMAT_STRING_VIEW".to_string(),
            Type::DiplomatOption(inner) => {
                let inner_layout = self.field_layout_element(inner);
                let (_inner_size, inner_align) = self.field_size_align(inner.as_ref());
                let padding_after_bool = inner_align.saturating_sub(1);
                if padding_after_bool > 0 {
                    format!("MemoryLayout.structLayout({inner_layout}.withName(\"value\"), ValueLayout.JAVA_BOOLEAN.withName(\"is_ok\"), MemoryLayout.paddingLayout({padding_after_bool}))")
                } else {
                    format!("MemoryLayout.structLayout({inner_layout}.withName(\"value\"), ValueLayout.JAVA_BOOLEAN.withName(\"is_ok\"))")
                }
            }
            _ => "ValueLayout.JAVA_BYTE".to_string(),
        }
    }

    fn field_java_type<P: hir::TyPosition>(&self, ty: &Type<P>) -> String {
        match ty {
            Type::Slice(slc) => match slc {
                Slice::Str(_, _) => "String".to_string(),
                Slice::Primitive(_, prim) => {
                    format!("{}[]", self.formatter.fmt_primitive_as_java(*prim))
                }
                _ => "Object".to_string(),
            },
            Type::DiplomatOption(inner) => match inner.as_ref() {
                Type::Primitive(prim) => {
                    self.formatter.fmt_primitive_as_java_boxed(*prim).to_string()
                }
                Type::Slice(slc) => match slc {
                    Slice::Str(_, _) => "String".to_string(),
                    Slice::Primitive(_, prim) => {
                        format!("{}[]", self.formatter.fmt_primitive_as_java(*prim))
                    }
                    _ => "Object".to_string(),
                },
                _ => self.type_to_java_name(inner.as_ref()),
            },
            _ => self.type_to_java_name(ty),
        }
    }

    /// Generate the expression to read a field from a MemorySegment in fromNative.
    /// `shouty` is the SHOUTY_SNAKE_CASE version of the field name, used to reference VarHandles/offsets.
    fn field_from_native<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        field_name: &str,
        shouty: &str,
    ) -> String {
        match ty {
            Type::Primitive(prim) => {
                let java_type = self.formatter.fmt_primitive_as_java(*prim);
                let vh = format!("VH_{shouty}");
                format!("({java_type}) {vh}.get(seg, 0L)")
            }
            Type::Opaque(_) => {
                let type_name = self.fmt_type_name_str(ty);
                let vh = format!("VH_{shouty}");
                format!("new {type_name}((MemorySegment) {vh}.get(seg, 0L))")
            }
            Type::Enum(_) => {
                let type_name = self.fmt_type_name_str(ty);
                let vh = format!("VH_{shouty}");
                format!("{type_name}.fromNative((int) {vh}.get(seg, 0L))")
            }
            Type::Struct(_) => {
                let type_name = self.fmt_type_name_str(ty);
                let offset_const = format!("OFFSET_{shouty}");
                format!(
                    "{type_name}.fromNative(seg.asSlice({offset_const}, {type_name}.LAYOUT.byteSize()))"
                )
            }
            Type::Slice(slc) => {
                let vh_data = format!("VH_{shouty}_DATA");
                let vh_len = format!("VH_{shouty}_LEN");
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
                            "new String(((MemorySegment) {vh_data}.get(seg, 0L)).reinterpret((long) {vh_len}.get(seg, 0L){byte_multiplier}).toArray(ValueLayout.JAVA_BYTE), {charset})"
                        )
                    }
                    Slice::Primitive(_, prim) => {
                        let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                        let (elem_size, _) = self.formatter.primitive_size_align(*prim);
                        format!(
                            "((MemorySegment) {vh_data}.get(seg, 0L)).reinterpret((long) {vh_len}.get(seg, 0L) * {elem_size}L).toArray({layout})"
                        )
                    }
                    _ => format!("null /* unsupported slice field {field_name} */"),
                }
            }
            Type::DiplomatOption(inner) => {
                let vh_is_ok = format!("VH_{shouty}_IS_OK");
                let inner_expr = match inner.as_ref() {
                    Type::Struct(_) => {
                        // Nested struct option uses offset + inner struct size
                        let type_name = self.fmt_type_name_str(inner.as_ref());
                        let offset_const = format!("OFFSET_{shouty}");
                        format!(
                            "{type_name}.fromNative(seg.asSlice({offset_const}, {type_name}.LAYOUT.byteSize()))"
                        )
                    }
                    Type::Slice(slc) => {
                        let vh_data = format!("VH_{shouty}_DATA");
                        let vh_len = format!("VH_{shouty}_LEN");
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
                                    "new String(((MemorySegment) {vh_data}.get(seg, 0L)).reinterpret((long) {vh_len}.get(seg, 0L){byte_multiplier}).toArray(ValueLayout.JAVA_BYTE), {charset})"
                                )
                            }
                            Slice::Primitive(_, prim) => {
                                let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                                let (elem_size, _) = self.formatter.primitive_size_align(*prim);
                                format!(
                                    "((MemorySegment) {vh_data}.get(seg, 0L)).reinterpret((long) {vh_len}.get(seg, 0L) * {elem_size}L).toArray({layout})"
                                )
                            }
                            _ => format!("null /* unsupported option slice field {field_name} */"),
                        }
                    }
                    _ => {
                        // Primitive, enum, opaque option uses VH_X_VALUE
                        let vh_value = format!("VH_{shouty}_VALUE");
                        self.field_from_native_via_vh(inner.as_ref(), &vh_value)
                    }
                };
                format!("(boolean) {vh_is_ok}.get(seg, 0L) ? {inner_expr} : null")
            }
            _ => format!("null /* unsupported field {field_name} */"),
        }
    }

    /// Generate expression to read a value from a VarHandle (for use inside DiplomatOption).
    fn field_from_native_via_vh<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        vh_name: &str,
    ) -> String {
        match ty {
            Type::Primitive(prim) => {
                let java_type = self.formatter.fmt_primitive_as_java(*prim);
                format!("({java_type}) {vh_name}.get(seg, 0L)")
            }
            Type::Enum(_) => {
                let type_name = self.fmt_type_name_str(ty);
                format!("{type_name}.fromNative((int) {vh_name}.get(seg, 0L))")
            }
            Type::Opaque(_) => {
                let type_name = self.fmt_type_name_str(ty);
                format!("new {type_name}((MemorySegment) {vh_name}.get(seg, 0L))")
            }
            _ => format!("{vh_name}.get(seg, 0L)"),
        }
    }

    /// Generate the statement to write a field to a MemorySegment in toNative.
    /// `shouty` is the SHOUTY_SNAKE_CASE version of the field name, used to reference VarHandles/offsets.
    fn field_to_native<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        field_name: &str,
        shouty: &str,
    ) -> String {
        match ty {
            Type::Primitive(_) => {
                let vh = format!("VH_{shouty}");
                format!("{vh}.set(seg, 0L, this.{field_name});")
            }
            Type::Opaque(_) => {
                let vh = format!("VH_{shouty}");
                format!("{vh}.set(seg, 0L, this.{field_name}.handle);")
            }
            Type::Enum(_) => {
                let vh = format!("VH_{shouty}");
                format!("{vh}.set(seg, 0L, this.{field_name}.toNative());")
            }
            Type::Struct(_) => {
                let type_name = self.fmt_type_name_str(ty);
                let offset_const = format!("OFFSET_{shouty}");
                format!(
                    "seg.asSlice({offset_const}, {type_name}.LAYOUT.byteSize()).copyFrom(this.{field_name}.toNative(arena));"
                )
            }
            Type::Slice(slc) => {
                let vh_data = format!("VH_{shouty}_DATA");
                let vh_len = format!("VH_{shouty}_LEN");
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
                            "{{ byte[] {field_name}Bytes = this.{field_name}.getBytes({charset}); var {field_name}Seg = arena.allocateFrom({elem_layout}, {field_name}Bytes); {vh_data}.set(seg, 0L, {field_name}Seg); {vh_len}.set(seg, 0L, (long) {len_expr}); }}"
                        )
                    }
                    Slice::Primitive(_, prim) => {
                        let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                        format!(
                            "{{ var {field_name}Seg = arena.allocateFrom({layout}, this.{field_name}); {vh_data}.set(seg, 0L, {field_name}Seg); {vh_len}.set(seg, 0L, (long) this.{field_name}.length); }}"
                        )
                    }
                    _ => format!("// unsupported slice field {field_name}"),
                }
            }
            Type::DiplomatOption(inner) => {
                let vh_is_ok = format!("VH_{shouty}_IS_OK");
                let inner_to_native = self.field_to_native_value(inner.as_ref(), field_name, shouty);
                format!(
                    "if (this.{field_name} != null) {{ {inner_to_native} {vh_is_ok}.set(seg, 0L, true); }} else {{ {vh_is_ok}.set(seg, 0L, false); }}"
                )
            }
            _ => format!("// unsupported field {field_name}"),
        }
    }

    /// Generate the statement to write a field's inner value to a MemorySegment (for Option unwrapping).
    /// `shouty` is the SHOUTY_SNAKE_CASE version of the field name.
    fn field_to_native_value<P: hir::TyPosition>(
        &self,
        ty: &Type<P>,
        field_name: &str,
        shouty: &str,
    ) -> String {
        match ty {
            Type::Primitive(_) | Type::Opaque(_) => {
                let vh = format!("VH_{shouty}_VALUE");
                format!("{vh}.set(seg, 0L, this.{field_name});")
            }
            Type::Enum(_) => {
                let vh = format!("VH_{shouty}_VALUE");
                format!("{vh}.set(seg, 0L, this.{field_name}.toNative());")
            }
            Type::Struct(_) => {
                let type_name = self.fmt_type_name_str(ty);
                let offset_const = format!("OFFSET_{shouty}");
                format!(
                    "seg.asSlice({offset_const}, {type_name}.LAYOUT.byteSize()).copyFrom(this.{field_name}.toNative(arena));"
                )
            }
            Type::Slice(slc) => {
                let vh_data = format!("VH_{shouty}_DATA");
                let vh_len = format!("VH_{shouty}_LEN");
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
                            "byte[] {field_name}Bytes = this.{field_name}.getBytes({charset}); var {field_name}Seg = arena.allocateFrom(ValueLayout.JAVA_BYTE, {field_name}Bytes); {vh_data}.set(seg, 0L, {field_name}Seg); {vh_len}.set(seg, 0L, (long) {len_expr});"
                        )
                    }
                    Slice::Primitive(_, prim) => {
                        let layout = self.formatter.fmt_primitive_as_ffi(*prim);
                        format!(
                            "var {field_name}Seg = arena.allocateFrom({layout}, this.{field_name}); {vh_data}.set(seg, 0L, {field_name}Seg); {vh_len}.set(seg, 0L, (long) this.{field_name}.length);"
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

    #[test]
    fn test_opaque_iterator() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct MyIterator(());

                impl MyIterator {
                    #[diplomat::attr(auto, iterator)]
                    pub fn next(&mut self) -> Option<i32> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_opaque_iterable() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct MyIterator(());

                impl MyIterator {
                    #[diplomat::attr(auto, iterator)]
                    pub fn next(&mut self) -> Option<Box<MyIterator>> {
                        unimplemented!()
                    }
                }

                #[diplomat::opaque]
                struct MyCollection(());

                impl MyCollection {
                    #[diplomat::attr(auto, iterable)]
                    pub fn iter(&self) -> Box<MyIterator> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    fn gen_all_with_traits_for_test(tk_stream: proc_macro2::TokenStream) -> String {
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
        for (_id, trt_def) in tcx.all_traits() {
            if trt_def.attrs.disable {
                continue;
            }
            let trait_name = trt_def.name.to_string();
            let (_file, body) = cx.gen_trait_def(trt_def, &trait_name);
            result.push_str(&body);
            result.push('\n');
        }
        result
    }

    #[test]
    fn test_opaque_indexer() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                #[diplomat::opaque]
                struct MyVec(());

                impl MyVec {
                    #[diplomat::attr(auto, indexer)]
                    pub fn get(&self, i: usize) -> Option<f64> {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_opaque_for_test(tk_stream));
    }

    #[test]
    fn test_callback_simple() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct CallbackWrapper {
                    cant_be_empty: bool,
                }

                impl CallbackWrapper {
                    pub fn test_multi_arg_callback(f: impl Fn(i32) -> i32, x: i32) -> i32 {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_struct_for_test(tk_stream));
    }

    #[test]
    fn test_callback_no_args() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct CallbackWrapper {
                    cant_be_empty: bool,
                }

                impl CallbackWrapper {
                    pub fn test_no_args(h: impl Fn()) -> i32 {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_struct_for_test(tk_stream));
    }

    #[test]
    fn test_callback_with_struct_param() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct SomeStruct {
                    x: i32,
                    y: i32,
                }

                pub struct CallbackWrapper {
                    cant_be_empty: bool,
                }

                impl CallbackWrapper {
                    pub fn test_cb_with_struct(f: impl Fn(SomeStruct) -> i32) -> i32 {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_for_test(tk_stream));
    }

    #[test]
    fn test_multiple_callbacks() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct CallbackWrapper {
                    cant_be_empty: bool,
                }

                impl CallbackWrapper {
                    pub fn test_multiple_cb_args(f: impl Fn() -> i32, g: impl Fn(i32) -> i32) -> i32 {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_struct_for_test(tk_stream));
    }

    #[test]
    fn test_trait_simple() {
        let tk_stream = quote! {
            #[diplomat::bridge]
            mod ffi {
                pub struct TraitTestingStruct {
                    x: i32,
                    y: i32,
                }

                pub trait TesterTrait {
                    fn test_trait_fn(&self, x: u32) -> u32;
                    fn test_void_trait_fn(&self);
                    fn test_struct_trait_fn(&self, s: TraitTestingStruct) -> i32;
                }

                pub struct TraitWrapper {
                    cant_be_empty: bool,
                }

                impl TraitWrapper {
                    pub fn test_with_trait(t: impl TesterTrait, x: i32) -> i32 {
                        unimplemented!()
                    }

                    pub fn test_trait_with_struct(t: impl TesterTrait) -> i32 {
                        unimplemented!()
                    }
                }
            }
        };

        insta::assert_snapshot!(gen_all_with_traits_for_test(tk_stream));
    }
}
