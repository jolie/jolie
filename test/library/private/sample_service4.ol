from console import Console
from .SampleInterface3 import TmpInterface3

service Test4 {

  embed Console as Console

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
}
