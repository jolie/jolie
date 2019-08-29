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
	// list@File( { .directory = "../dist/jolie/lib/jaxws" } )( list );
	// for( path in list.result ) {
	// 	toAbsolutePath@File( "../dist/jolie/lib/jaxws/" + path )( path );
	// 	loadLibrary@Runtime( path )()
	// };
	loadLibrary@Runtime( "extensions/private/WS-test.jar" )();
	loadEmbeddedService@Runtime
		( { .filepath = "ws.test.WSTest", .type = "Java" } )
		( CalcServiceJoliePort.location )
}
