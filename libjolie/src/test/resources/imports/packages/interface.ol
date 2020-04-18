
// inline type
interface twiceIface{
    requestResponse: twice(int)(int)
}

interface aIface{
    oneWay: notice(string)
}

interface bIface{
    requestResponse: twice(int)(int)
    oneWay: notice(string)
}

// custom type
type foo: void{
    a: string
}
type bar: void{
    b: string
}
type err: void{
    msg:string
}
interface fooIface{
    requestResponse: fooOp(foo)(bar) throws err(err)
}