from ..test-unit import TestUnitInterface
from .private.TypeLink_for_extend import typeInOtherFile



type TestType {
  header: string
}

interface NothingServiceInterface {
  requestResponse:
    nothing( TestType )( void )
}

service NothingService {

    inputPort TaskService2 {
      location: "local"
      interfaces: NothingServiceInterface
    }

    main {
        nothing( req )( res ) { nullProcess }
    }
}

interface extender TokenExtender {
  RequestResponse:
    *( typeInOtherFile )( typeInOtherFile ) throws InvalidToken
}

service EmbedderService {

  embed NothingService as NothingService

  inputPort input {
    location: "local://EmbedderServiceTypeLink"
    aggregates: NothingService with TokenExtender
  }

  courier input {
    [ nothing( request )( response ){
      forward( request )( response )
      response.token = 2
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
    nothing( TestWithTokenType )( typeInOtherFile ) throws InvalidToken
}

service Main {

    embed EmbedderService

    outputPort EmbedderService {
        location: "local://EmbedderServiceTypeLink"
        interfaces: NothingServiceInterfaceWithToken
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
            })(r)
            if ( r.token != 2 ) {
              throw( TestFailed, "expect response.token to be " + 2 + ", got " + r.token )
            }
        }
    }

}

