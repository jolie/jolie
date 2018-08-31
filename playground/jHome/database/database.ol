include "database.iol"
include "../common/locations.iol"
include "console.iol"
include "runtime.iol"


inputPort DatabaseInput {
Location: Location_JHomeDatabase
Protocol: sodep
Aggregates: Database
}

init
{
	loadLibrary@Runtime( "file:lib/derby.jar" )();
	scope( JHomeDatabaseConnectionScope ) {
		install( IOException => println@Console( JHomeDatabaseConnectionScope.IOException.stackTrace )() );
		install( ConnectionError => println@Console( JHomeDatabaseConnectionScope.ConnectionError )() );
		with( connectionInfo ) {
			.host = "";
			.driver = "derby_embedded";
			.port = 0;
			.database = "database/data/jhome";
			.username = "";
			.password = ""
		};
		connect@Database( connectionInfo )()
	}
}

main
{
	linkIn( fake )
}
