type T1: string

type T2: int

type T3: string

type T4: int

interface TmpInterface1 {
    RequestResponse:
        test1( T1 )( T2 )
}

interface TmpInterface2 {
    RequestResponse:
        test2( T3 )( T4 )
}