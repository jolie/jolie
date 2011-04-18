include "frontend.iol"
include "console.iol"
include "string_utils.iol"

execution { concurrent }

inputPort JHomeFrontendInput {
Location: "local"
Interfaces: JHomeFrontendInterface
}

init
{
	scope( JHomeDatabaseConnectionScope ) {
		with( connectionInfo ) {
			.host = "";
			.driver = "derby_embedded";
			.port = 0;
			.database = "../db/jhome";
			.username = "";
			.password = ""
		};
		connect@Database( connectionInfo )()
	}
}

main
{
	getPageTitle( request )( "Hello " + request.hello ) {
		valueToPrettyString@StringUtils( request )( response );
		println@Console( response )()
	}
}
