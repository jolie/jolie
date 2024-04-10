from .assertions import Assertions
from ..greeter import GreeterAPI

interface TestInterface {
RequestResponse:
    /// @Test
    test1(void)(void) throws AssertionError(string),
    /// @Test
    test2(void)(void) throws AssertionError(string)
}

type TestParams {
    location: string
    protocol: string
}

service TestGreeter( params:TestParams ) {

	embed Assertions as assertions
    
    outputPort Greeter {
        location: params.location
        protocol: params.protocol
        interfaces: GreeterAPI
    }

	execution: sequential

	inputPort Input {
		location: "local"
		interfaces: TestInterface
	}

    main {
        [ test1()(){
            greet@Greeter( { name = "Alice" } )( response )
            equals@assertions({
                actual = response
                expected = { greeting = "Hello, Alice" }
            })()
        }]
        [ test2()(){
            greet@Greeter( { name = "Bob" } )( response1 ) |
            greet@Greeter( { name = "Bob" } )( response2 )
            equals@assertions({
                actual = response1
                expected = response2
                message = "expected same response, for '" + response1 + "' and '" + response2 + "'"
            })()
        }]
    }
}