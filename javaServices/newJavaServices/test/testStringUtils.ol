from assertions import Assertions
from string-utils import StringUtils
from ..packages.string-utils import StringUtils as NewStringUtils

interface MyTestInterface {
RequestResponse:
	///@Test
	testToLowerCase(void)(void) throws TestFailed(string),
	///@Test
	testLength(void)(void) throws TestFailed(string)

}

service main( ) {

	embed Assertions as assertions
	embed StringUtils as stringUtils
	embed NewStringUtils as newStringUtils
    
	execution: sequential

	inputPort Input {
		location: "local"
		interfaces: MyTestInterface
	}

    main {
		[ testToLowerCase()() {
			scope ( lowercase ) {

				request = "AbC dEf_GhI"

				toLowerCase@stringUtils( request )( response.expected ) 
				toLowerCase@newStringUtils( request )( response.actual )
				
				install ( default => 
					throw( AssertionError, "expected '" + response.expected + "', got '" + response.actual + "'." ) 
				)

				equals@assertions( response )()
			}
        } ]

        [ testLength()() {
			scope ( length ) {

				request = "12345678"
				
				length@stringUtils( request )( response.expected )
				length@newStringUtils( request )( response.actual )
				
				install ( default => 
					throw( AssertionError, "expected " + response.expected + ", got " + response.actual + "." ) 
				)

				equals@assertions( response )()
			}
		} ]
    }
}