include "runtime.iol"
include "file.iol"

type RequestType:void {
	.x?:int
	.y?:int
}

type ResponseType:void {
	.return?:int
}

interface CalcService {
	RequestResponse:
	sum( RequestType )( ResponseType ),
	prod( RequestType )( ResponseType )
}

outputPort CalcServicePort {
	Location: "socket://localhost:14000/"
	Protocol: soap {
		.wsdl = "extensions/private/WS-test/lib/WS-test.wsdl";
		.wsdl.port = "CalcServicePort"
	}
	Interfaces: CalcService
}

outputPort CalcServiceJoliePort {
	RequestResponse:
		start( string )( void ),
		close( void )( void )
}

define loadLocalService
{
	libFolder = "extensions/private/WS-test/lib/lib"
	list@File( { .directory = libFolder } )( list )
	 for( file in list.result ) {
	 	toAbsolutePath@File( libFolder + "/" + file )( path )
	 	loadLibrary@Runtime( path )()
	 };
	loadLibrary@Runtime( "extensions/private/WS-test.jar" )()
	loadEmbeddedService@Runtime
		( { .filepath = "ws.test.SumService", .type = "Java" } )
		( CalcServiceJoliePort.location )
	
}
