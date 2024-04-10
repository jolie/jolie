from assertions import Assertions
from time import Time
from ..packages.time import Time as NewTime

interface MyTestInterface {
RequestResponse:
	///@Test
	testGetDateValues(void)(void) throws TestFailed(string)

}

service main( ) {

	embed Assertions as assertions
	embed Time as time
	embed NewTime as newTime
    
	execution: sequential

	inputPort Input {
		location: "local"
		interfaces: MyTestInterface
	}

    main {
		[ testGetDateValues()() {
			scope( getdatevalues ) {
				getCurrentDateTime@time()( request )
				getDateValues@time( request )( response.expected )
				getDateValues@newTime( request )( response.actual )

				install( default => 
					throw( TestFailed, "expected " + response.expected + ", got " + response.actual + " instead." )
				)

				equals@assertions( response )()
			}
		} ]
    }
}