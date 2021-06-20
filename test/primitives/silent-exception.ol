include "../AbstractTestUnit.iol"
include "runtime.iol"
include "console.iol"
include "time.iol"


outputPort Test {
    RequestResponse: test, test2
}

inputPort Client {
	Location: "socket://localhost:7998"
	Protocol: sodep 
	RequestResponse: test2
}

inputPort Local {
	Location: "local"
	Protocol: sodep
	OneWay: alarm
}

define doTest
{

    rq.code = "
include \"console.iol\"
include \"time.iol\"
include \"runtime.iol\"

execution{ concurrent }

outputPort Client {
	Location: \"socket://localhost:7998\"
	Protocol: sodep 
	RequestResponse: test2
}

inputPort Test {
	Location: \"local\"
	Protocol: sodep 
	RequestResponse: test, test2 
}


main
{

	[ test( request )( response ) {
		nullProcess
	}] {
		scope( error ) {
			install( default => nullProcess )
			a[ 0 ] = 1
			h = a[ -1 ]
		}
		test2@Client( request )( response )
	}
 
}
    "

    loadEmbeddedService@Runtime( rq )( Test.location )
    
    test@Test()()
    setNextTimeout@Time( 1000 { operation = "alarm" } )

    [ test2()() ] 
    [ alarm() ] {
        throw( TestFailed, "A silent exception blocked the test service")
    }

}