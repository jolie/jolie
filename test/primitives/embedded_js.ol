include "../AbstractTestUnit.iol"

type TwiceRequest:void {
	.number: int
}

type PowRequest:void {
	.x:double
	.y:double
}

interface TwiceInterface {
RequestResponse:
	twice( TwiceRequest )( double ),
	pow( PowRequest )( double )
}

outputPort TwiceService {
Interfaces: TwiceInterface
}

embedded {
JavaScript:
	"primitives/private/TwiceService.js" in TwiceService
}

define doTest
{
	request.number = 5;
	twice@TwiceService( request )( response );
	if ( int( response ) != 10 ) {
		throw( TestFailed, "wrong result for twice" )
	};

	pow@TwiceService( { .x = 3, .y = 4 } )( result );
	if ( int( result ) != 81 ) {
		throw( TestFailed, "wrong result for pow" )
	}
}
