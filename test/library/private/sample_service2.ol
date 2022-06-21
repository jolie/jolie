from console import Console
from .SampleInterface import TmpInterface

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

service Test2 {

  embed Console as Console

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
}
