from console import Console
from .SampleInterface2 import TmpInterface2

service Test3 {

  embed Console as Console

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
}
