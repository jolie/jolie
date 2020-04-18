// inline test
type date: void{
    day: int
    month: int
    year: int
}

// choice test
type number: int | double

type foo: void{
    a:int
}
type bar: void{
    b:int
}

type baz: foo | bar

// link type
type dateFoo: void{
    date: date
    foo: foo
}