include "SampleInterface2.iol"
include "console.iol"

inputPort TPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TmpInterface2
}

main {
  tmp()( response ) {
    response.field = "test";
    print@Console("")()
  }
}
