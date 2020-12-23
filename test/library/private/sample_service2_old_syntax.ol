include "SampleInterface.ol"
include "console.iol"

type Extension: void {
	.ext: void {
		.field_ext: string 
	}
}

interface extender Extender {
  RequestResponse:
      *( Extension )( void )
}

interface TmpInterface2 {
    RequestResponse:
        test2( string )( string )
}

outputPort TPort2 {
    Interfaces: TmpInterface
}

inputPort TPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TmpInterface2
  Aggregates: TPort2 with Extender
}

main {
  tmp()( response ) {
    response.field = "test";
    print@Console("")()
  }
}
