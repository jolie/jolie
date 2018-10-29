include "server.iol"

execution { single }

inputPort Server {
  Location: Location_CoAPServer
  Protocol: coap {
    // .debug = true;
    .timeout = 2;
    .osc.echoPerson << {
      .alias = "echoPerson",
      // .separateResponse = true,
      .messageType = "NON",
      .messageCode = "CONTENT"
    };
    .osc.identity << {
      .alias = "identity",
      // .separateResponse = true,
      .messageType = "NON",
      .messageCode = "CONTENT"
    };
    .osc.shutdown << { 
      .alias = "shutdown"
    }
  }
  Interfaces: ServerInterface 
}

main 
{
  provide
    [ echoPerson( request )( response ) {
      undef( response );
      response << request
    } ]
    [ identity( request )( response ) {
      undef( response );
      response << request
    } ]
  until
    [ shutdown() ]
}