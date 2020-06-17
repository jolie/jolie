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
    oneWay: bar(bar)
    requestResponse: fooOp(foo)(bar) throws err(err)
}