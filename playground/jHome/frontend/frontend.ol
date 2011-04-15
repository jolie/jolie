include "frontend.iol"
include "console.iol"
include "string_utils.iol"

execution { concurrent }

inputPort JHomeFrontendInput {
Location: "local"
Interfaces: JHomeFrontendInterface
}

main
{
	getPageTitle( request )( "Hello " + request.hello ) {
		valueToPrettyString@StringUtils( request )( response );
		println@Console( response )()
	}
}
