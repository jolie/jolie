from ..test-unit import TestUnitInterface

include "math.iol"
include "console.iol"

service Test {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

    
	main {
		test()() {
            request << {
                from = 1,
                to = 1,
            }

            sum = summation@Math( request )

            if( sum != 1 ) {
                throw(TestFailed, "return value does not match expected value")
            }

            request.to = 2
            sum = abs@Math( summation@Math( request ) )
            if( sum != 3 ) {
                throw(TestFailed, "doesn't work when nested")
            }

            request.to = 1
            sum = summation@Math( request ) * 1 + ( summation@Math( request ) + 1 ) * 2
            if( sum != 5 ) {
                throw(TestFailed, "doesn't work with multiple terms and factors")
            }
			
		}
	}
}
