

interface frontendInterface {
    RequestResponse:
        testFr( void )( undefined )
}

service frontend {

    inputPort Frontend {
        location: "local"
        protocol: sodep
        interfaces: frontendInterface
    }

    main {
        testFr( request )( response ) {
            response.a.b.c = "hello"
        }
    }
}


interface testInterface {
    RequestResponse:
        test 
}

service test {

    embed frontend as fe

    inputPort HTTP {
        location: "socket://localhost:55555"
        protocol: http 
        interfaces:  testInterface
        aggregates: fe
    }

    main {
        test( request )( response ) {
           nullProcess
        }
    }
}