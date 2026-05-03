#include "diplomat_nanobind_common.hpp"


#include "ns/RenamedPartialComparable.hpp"

namespace somelib::ns {
void add_RenamedPartialComparable_binding(nb::module_ mod) {
    PyType_Slot somelib_ns_RenamedPartialComparable_slots[] = {
        {Py_tp_free, (void *)somelib::ns::RenamedPartialComparable::operator delete },
        {Py_tp_dealloc, (void *)diplomat_tp_dealloc},
        {0, nullptr}};
    
    nb::class_<somelib::ns::RenamedPartialComparable> opaque(mod, "RenamedPartialComparable", nb::type_slots(somelib_ns_RenamedPartialComparable_slots));
    opaque
        .def(nb::new_(std::move(maybe_op_unwrap(&somelib::ns::RenamedPartialComparable::new_))), "float_"_a)
        .def(nb::self == nb::self)
            .def(nb::self != nb::self)
            .def(nb::self <= nb::self)
            .def(nb::self >= nb::self)
            .def(nb::self < nb::self)
            .def(nb::self > nb::self)
        .def("test_nonstd", &somelib::ns::RenamedPartialComparable::test_nonstd, "other"_a);
}

} 