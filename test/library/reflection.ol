include "../AbstractTestUnit.iol"
include "reflection.iol"
include "math.iol"

define doTest
{
	invoke@Reflection( {
		.operation = "abs",
		.outputPort = "Math",
		.data = -5
	} )( result );
	if ( result != 5 ) {
		throw( TestFailed, "invoke@Reflection result (" + result + ") does not match expected value (5)" )
	}
}
