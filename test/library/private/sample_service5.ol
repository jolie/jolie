from console import Console
from .SampleInterface4 import TmpInterface4

service Test5 {
  embed Console as Console
  
  inputPort TPort {
    Location: "socket://localhost:9000"
    Protocol: sodep
    Interfaces: TmpInterface4
  }

  main {
    tmp()( response ) {
      response.field = "test";
      print@Console("")()
    }
  }
}