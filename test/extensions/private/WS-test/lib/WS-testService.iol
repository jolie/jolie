include "exec.iol"
include "time.iol"

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

embedded {
  Java: "ws.test.WSTest" in CalcServiceJoliePort
}