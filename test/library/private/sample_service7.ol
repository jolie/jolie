
from library.private.SampleInterface6 import TmpInterface1
from library.private.SampleInterface6 import TmpInterface2

service Test6 {
  outputPort Op1 {
    Location: "socket://localhost:9001"
    Protocol: sodep
    Interfaces: TmpInterface1
  }

  outputPort Op2 {
    Location: "socket://localhost:9001"
    Protocol: sodep
    Interfaces: TmpInterface2
  }
  
  
  inputPort TPort {
    Location: "socket://localhost:9000"
    Protocol: sodep
    Interfaces: TmpInterface1
  }

  inputPort TPort2 {
    Location: "socket://localhost:9005"
    Protocol: sodep
    Interfaces: TmpInterface2
  }

  main {
    test1()( response ) {
      response.field = "test"
    }
  }
}