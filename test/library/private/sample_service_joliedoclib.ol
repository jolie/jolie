
from .SampleInterface7 import TmpInterface7

service Test {

  inputPort TPort {
    Location: "socket://localhost:9005"
    Protocol: sodep
    Interfaces: TmpInterface7
  }

  main {
    [ tmp()( response ) {
      response.field = "test"
    }]

    [ tmp2()( response ) {
      response.field = "test"
    }]

    [ tmp3()( response ) {
      response.field = "test"
    }]
  }
}

