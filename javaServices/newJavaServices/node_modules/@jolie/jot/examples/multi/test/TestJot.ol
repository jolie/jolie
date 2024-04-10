from assertions import Assertions
from console import Console
from string-utils import StringUtils

interface MyTestInterface {
RequestResponse:

	///@Test
	testSimple(void)(void) throws TestFailed(string)
}

service main( ) {

	embed Assertions as assertions
	embed Console as console
	embed StringUtils as stringUtils
	execution: sequential

	inputPort Input {
		location: "local"
		interfaces: MyTestInterface
	}

    main{
        
		[ testSimple()() {
			scope(test){
				install( default => 
					throw( TestFailed, "expected 2" )
				)
				equals@assertions({
					actual = 2
					expected = 2
				})()
			}
		} ]
    }
}