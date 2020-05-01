type T1: void

/**!documentation of type T2*/
type T2: void {
  .field:string //<documentation of field
  .recursion?: T3
}

type T3: void {
  .fieldChoice: double
}
|
string {
  .fieldChoice2: string
  .fieldChoice3: T2
}

type T4: T1 | T2

type T5: void {
  .fieldChoice: double
}
|
string {
  .fieldChoice2: string
}
|
int {
  .fieldChoice3: raw
}

type T6: void | string | int 

type T7: undefined

type T8:  void {
  .fieldChoice: double
}

type T9: void {
  .field:string 
}

type T10: void {
  .fieldChoice: double
}
|
string {
  .fieldChoice2: string
}
|
int {
  .fieldChoice3: raw
}
|
string {
  .fieldChoice4: int
} 

type T11: void {
  .fieldChoice: double
  .field11?: string
}
|
string {
  .fieldChoice2: string
  .fieldChoice3: T2
}

type T12: T3

type T13: T11

/**! documentation of interface */
interface TmpInterface4 {
  RequestResponse:
    /**!documentation of operation tmp*/
    tmp( T1 )( T2 ) throws Fault1( T11 ),
    /**!documentation of operation tmp2*/
    tmp2( T3 )( T4 ) throws Fault2 fault3,
    tmp3( T10 )( T6 ) throws Fault3( string ) Fault4( T7 ) Fault5( T11 ) Faults6( T12 ) Fault7( T13 ),
    tmp4( T8 )( undefined )
}