from .B_dup import b_type as b_dup_imported, b_linked_type
from .B import b_type as b_imported

main
{
    t = {b_subtype = "test"} instanceof b_imported
    t = {b_subtype = "test"} instanceof b_dup_imported
    t = {b_subtype = "test"} instanceof b_linked_type
}