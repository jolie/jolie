include "SampleInterface5.ol"
include "console.iol"

inputPort TPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TmpInterface5
}

main {
  tmp()( response ) {
    response.field = "test";
    print@Console("")()
  }
}
