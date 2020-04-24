from .B import b_type as b_imported

type from_b: b_imported

main
{
    t = {b_subtype = "test"} instanceof from_b
}