from type import *
from interface import fooIface
from twice import *


outputPort OP {
    interfaces: TwiceAPI
}


outputPort OP2 {
    interfaces: fooIface
}

main{
    twice@OP(2)()
    t = 2 instanceof number
    t = { a=2 } instanceof foo
}