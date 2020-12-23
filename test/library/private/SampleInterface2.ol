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

/**! documentation of interface */
interface TmpInterface2 {
  RequestResponse:
    /**!documentation of operation tmp*/
    tmp( T1 )( T2 ) throws Fault1( T3 ),
    /**!documentation of operation tmp2*/
    tmp2( T3 )( T4 ) throws Fault2,
    tmp3( T5 )( T6 ) throws Fault3( string ) Fault4( T7 ),
    tmp4( T8 )( T9 )
}