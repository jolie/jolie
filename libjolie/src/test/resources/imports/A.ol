from .A.B import b_type as b_imported
type A {
    a_type: int
}
type A_linked {
    a_type: b_imported
}
type A_linked {
    a_type {
        a_type_1: b_imported
    }
}

type from_b: b_imported