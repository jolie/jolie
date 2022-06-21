from console import Console
from .SampleInterface5 import TmpInterface5

service Test5 {
  embed Console as Console
  
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
}