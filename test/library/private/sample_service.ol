type T1: void

type T2: void {
  .field:string
}

interface TmpInterface {
  RequestResponse:
    tmp( T1 )( T2 )
}

inputPort TPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TmpInterface
}

main {
  tmp()() {
    nullProcess
  }
}
