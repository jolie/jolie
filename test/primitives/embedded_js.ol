include "../AbstractTestUnit.iol"

type TwiceRequest:void {
	.number: int
}

interface TwiceInterface {
RequestResponse:
	twice( TwiceRequest )( int )
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
	if ( response != 10 ) {
		throw( TestFailed, "wrong result" )
	}
}
