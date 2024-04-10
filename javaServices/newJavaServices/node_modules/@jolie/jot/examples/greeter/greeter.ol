type GreetRequest { name:string }
type GreetResponse { greeting:string }

interface GreeterAPI {
    RequestResponse: greet( GreetRequest )( GreetResponse )
}

service Greeter {
    execution: concurrent

    inputPort GreeterInput {
        location: "socket://localhost:9000"
        protocol: jsonrpc
        interfaces: GreeterAPI
    }

    main {
        greet( request )( response ) {
            response.greeting = "Hello, " + request.name
        }
    }
}