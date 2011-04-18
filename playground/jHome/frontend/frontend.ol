include "frontend.iol"
include "console.iol"
include "string_utils.iol"
include "database.iol"

execution { concurrent }

inputPort JHomeFrontendInput {
Location: "local"
Interfaces: JHomeFrontendInterface
}

include "../common/jhome_db_connect.iol"

main
{
	getPageTitle( request )( "Hello " + request.hello ) {
		valueToPrettyString@StringUtils( request )( response );
		println@Console( response )()
	}
}
