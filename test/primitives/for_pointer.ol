include "../AbstractTestUnit.iol"
include "runtime.iol"
include "console.iol"
outputPort Test {
    RequestResponse: test, initialize
}

define doTest
{

    rq.code = "
    include \"console.iol\"
    execution{ concurrent }
    inputPort Test {
        Location: \"local\"
        Protocol: sodep
        RequestResponse: test, initialize
    }   

    init {
        initialize()()
    }


    main {
        test( request )( response ) {
            for( i=0, i < #request.term,i++ ) {
                p -> request.term[i]
                response = response + p
            }
        }
    }
    "

    loadEmbeddedService@Runtime( rq )( Test.location )
    initialize@Test()()

    sum = 0
    for( x = 0, x < 3, x++ ) {
        test_req.term[ x ] = x + 1
        sum = sum + x + 1
    }

// println@Console( "Sum " + sum )(  )
    for( x = 0, x < 50, x++ ) {
        vect[ x ] << test_req
    }

    spawn( y over #vect ) in resultVar {
        test@Test( vect[ y ] )( resultVar )       
    }

/* for( i = 0, i < #resultVar, i++ ) {
    println@Console( resultVar[ i ] )(  )
} */

    for( i = 0, i < #resultVar, i++ ) {
        if ( resultVar[ i ] != sum ) {
            throw( TestFailed, "Call " + i + ", expected " + sum + " found " + resultVar[ i ] )
        }
    }
}
