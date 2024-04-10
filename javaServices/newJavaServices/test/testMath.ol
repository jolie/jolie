from assertions import Assertions
from math import Math
from ..packages.math import Math as NewMath

interface MyTestInterface {
RequestResponse:
	///@Test
	testRound(void)(void) throws TestFailed(string),
	///@Test
	testPow(void)(void) throws TestFailed(string),
	///@Test
	testSummation(void)(void) throws TestFailed(string)

}

service main( ) {

	embed Assertions as assertions
	embed Math as math
	embed NewMath as newMath
    
	execution: sequential

	inputPort Input {
		location: "local"
		interfaces: MyTestInterface
	}

    main {
		[ testRound()() {
			scope( round ) {
				pi@newMath()( request )
				round@math( request )( response.expected )
				round@newMath( request )( response.actual )

				install( default => 
					throw( TestFailed, "expected " + response.expected + ", got " + response.actual + " instead." )
				)

				equals@assertions( response )()
			}
		} ]

		[ testPow()() {
			scope( pow ) {
				request << { base = 132.121849384, exponent = 4.2 }
				pow@math( request )( response.expected )
				pow@newMath( request )( response.actual )
				
				install( default => 
					throw( TestFailed, "expected " + response.expected + ", got " + response.actual + " instead." )
				)

				equals@assertions( response )()
			}
		} ]

		[ testSummation()() {
			scope( sum ) {
				request << { from = 1, to = 12 }
				summation@math( request )( response.expected )
				summation@newMath( request )( response.actual )
				
				install( default => 
					throw( TestFailed, "expected " + response.expected + ", got " + response.actual + " instead." )
				)

				equals@assertions( response )()
			}
		} ]
    }
}