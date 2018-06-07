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
  prod( RequestType )( ResponseType ),
  close( void )( string )
}

outputPort CalcServicePort {
  Location: "socket://localhost:14000/"
  Protocol: soap {
    .wsdl = "extensions/private/WS-test.wsdl";
    .wsdl.port = "CalcServicePort"
  }
  Interfaces: CalcService
}