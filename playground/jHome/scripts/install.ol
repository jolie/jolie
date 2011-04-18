include "database.iol"
include "runtime.iol"
include "console.iol"
//include "file.iol"

main
{
	install( IOException => println@Console( main.IOException.stackTrace )() );
	install( ConnectionError => println@Console( main.ConnectionError )() );
	loadLibrary@Runtime( "file:../lib/derby.jar" )();
	with( connectionInfo ) {
		.host = "";
		.driver = "derby_embedded";
		.port = 0;
		.database = "../db/jhome";
		.username = "";
		.password = "";
		.attributes = "create=true"
	};
	connect@Database( connectionInfo )()
}