from ..test-unit import TestUnitInterface


type TestType {
  header: string
}

interface NothingServiceInterface {
  requestResponse:
    nothing( TestType )( void )
}

service NothingService {

    inputPort TaskService2 {
      location: "local://NothingService"
      interfaces: NothingServiceInterface
    }

    main {
        nothing( req )( res ) { nullProcess }
    }
}

type TokenType { token: int }

interface extender TokenExtender {
  RequestResponse:
    *( TokenType )( TokenType ) throws InvalidToken
}

service EmbedderService {

  embed NothingService

  outputPort NothingService {
    Location: "local://NothingService"
    Interfaces: NothingServiceInterface
  }

  inputPort input {    
    location: "local://EmbedderService"
    aggregates: NothingService with TokenExtender
  }

  courier input {
    [ nothing( request )( response ){
      forward( request )( response )
      response.token = 1
    } ]
  }

  main {
    linkIn(l)
  }

}

type TestWithTokenType {
  header: string
  token: int
}

interface NothingServiceInterfaceWithToken {
  requestResponse:
    nothing( TestWithTokenType )( TokenType ) throws InvalidToken
}

service Main {

    embed EmbedderService

    outputPort EmbedderService {
      Location: "local://EmbedderService"
      Interfaces: NothingServiceInterfaceWithToken
    }

    inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {
            nothing@EmbedderService({
                header = "test"
                token = 1
            })()
        }
    }

}

