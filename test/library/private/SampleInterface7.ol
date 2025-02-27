type TNative {
    s1: string
    s2: int
    s3: long
    s4: any
    s5: raw
    s6: double
    s7: void
    s8: bool
    s9: string( length( [2,5] ) )
    s10: string( regex(".*@.*\\..*") )
    s11: string( enum(["paul","homer","mark"]))
    s12: int( ranges( [-*,-1], [1,4], [10,20], [100,200], [300, *]) )
    s13: long( ranges( [-*,-1L], [3L,4L], [10L,20L], [100L,200L], [300L, *]) )
    s14: double( ranges( [-*,-1.0], [4.0,5.0], [10.0,20.0], [100.0,200.0], [300.0, *]) )
}

type TInLine {      // doc TInLine
    i: int {        // doc field i
      k: TNative    // doc field k
      m: string     // doc field m
      n {           // doc field n
        j: string   // doc field j
        k: int      // doc field k
      }
      r: undefined  // doc field r
    }
}

type TLink {
  h: TInLine 
  m: TNative
}

type TChoice {
   a: string
} | int {
   b: TNative
}

type TSimple : void {
    s: string
}

/**! documentation of 


interface */
interface TmpInterface7 {
  RequestResponse:
    // /**!documentation of operation tmp*/
    tmp( TNative )( TNative ) throws Fault1( TNative ),
    /**!documentation of operation tmp2*/
    tmp2( TLink )( TLink ) throws Fault2,
    tmp3( TInLine )( TInLine ) throws Fault3( string ) Fault4( TInLine ),
    tmp4( TSimple )( TSimple ),
    tmp5( TChoice )( TChoice )
   
}

