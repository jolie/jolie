type T2 {
	r1: string {
		r2: int 
		r3: long {
			r4: double 
			r5: any
			r6: raw
		}
	}

}

type T1: void {
  	f1: string( length( [0,10] ) )
	f2: string( enum(["hello","homer","simpsons"]))
	f3: string( length( [0,20] ) )
	f4: int( ranges( [1,4], [10,20], [100,200], [300, *]) )
	f5: long( ranges( [3L,4L], [10L,20L], [100L,200L], [300L, *]) )
	f6: double( ranges( [4.0,5.0], [10.0,20.0], [100.0,200.0], [300.0, *]) )
	f7: string( regex(".*@.*\\..*") )
	f8: T2
}

interface TmpInterface {
  RequestResponse:
    tmp( T1 )( T1 )
}