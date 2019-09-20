include "SampleInterface.iol"
include "console.iol"

inputPort TPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TmpInterface
}

main {
  tmp()( response ) {
    response.field = "test"
  }
}
