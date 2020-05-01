from packages.type import *
from packages.interface import fooIface


outputPort OP {
    interfaces: fooIface
}


outputPort OP2 {
    interfaces: fooIface
}

main{
    fooOp@OP(2)()
    t = 2 instanceof number
    t = { a=2 } instanceof foo
}