include "SampleInterface3.ol"
include "console.iol"

inputPort TPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TmpInterface3
}

main {
  tmp()( response ) {
    response.field = "test";
    print@Console("")()
  }
}
