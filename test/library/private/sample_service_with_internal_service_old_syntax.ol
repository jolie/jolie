include "SampleInterface.ol"
include "console.iol"

inputPort TPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TmpInterface
}

type InternalMsg: string

interface InternalIFace {
  RequestResponse:
    internalop( InternalMsg )( InternalMsg )
}

service InternalService {
    Interfaces: InternalIFace
    main {
            internalop( req )( res ) {
              res = req
            }
    }
}

main {
  tmp()( response ) {
    response.field = "test";
    internalop@InternalService( "hello" )( res )
    print@Console("")()
  }
}
