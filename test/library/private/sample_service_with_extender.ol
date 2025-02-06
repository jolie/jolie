type type1 {
  thing: any
}

type type2 { ones*: type1 }

interface api {
  oneWay: 
    ow2( type2 )
  requestResponse:
    call1(void)(type2),
    call2(type1)(void),
    call3(type2)(type2) throws FaultEx( type1 )
}

interface api2 {
  oneWay:
    ow1(type1)
  requestResponse:
    call4(void)(type2),
    call5(type1)(void),
    call6(type2)(type2) throws FaultEx( type1 )
}

type type3 { extend: string }

interface extender TokenExtender {
  OneWay:
    ow2( type3 )
  RequestResponse:
    call2( type3 )( void ),
    call3( type3 )( type3 ) throws FaultEx( type3 )
}

interface extender TokenExtender2 {
  OneWay:
    *(type3)
  RequestResponse:
    *( type3 )( void )
}

service bug {

  outputPort output {
    location: "local"
    interfaces: api
  }

  outputPort output2 {
    location: "local"
    interfaces: api2
  }

  inputPort input {
    location: "local"
    aggregates: output with TokenExtender, output2 with TokenExtender2
  }

  courier input {
    [ interface api( request )( response ){
      forward( request )( response )
    } ]
  }

  main{ linkIn(l) }
}