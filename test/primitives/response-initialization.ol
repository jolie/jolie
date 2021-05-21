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
            for( t in request.term ) {
                response = response * t
            }
        }
    }
    "

    loadEmbeddedService@Runtime( rq )( Test.location )
    initialize@Test()()

    for( x = 0, x < 3, x++ ) {
        test_req.term[ x ] = x + 1
    }
    test@Test( test_req )( test_res )
 
    if ( test_res != 0 ) {
        throw( TestFailed, "Expected 0 found " + test_res )
    }
    



}