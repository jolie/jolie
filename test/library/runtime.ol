include "../AbstractTestUnit.iol"
include "runtime.iol"


outputPort Test {
    RequestResponse: test
}
define doTest
{
    request.code = "
    include \"string_utils.iol\"
    inputPort Test {
        Location: \"local\"
        Protocol: sodep
        RequestResponse: test
    }

    main {
        test( request )( response ) {
            valueToPrettyString@StringUtils( request )( s )
            response = request
        }
    }
    "
	loadEmbeddedService@Runtime( request )( Test.location )
    string_to_test = "hello"
    test@Test( string_to_test )( res )
	if ( res !=  string_to_test) {
		throw( TestFailed, "loadEmbeddedService@Runtime expected " + string_to_test + ", found " + res )
	}
}
