from assertions import Assertions
from console import Console
from string-utils import StringUtils

type params {
	name: string
}

interface MyTestInterface {
RequestResponse:
	///@Test
	testParams(void)(void) throws TestFailed(string)
}

service main( p : params ) {

	embed Assertions as assertions
	embed Console as console
	embed StringUtils as stringUtils
	execution: sequential

	inputPort Input {
		location: "local"
		interfaces: MyTestInterface
	}

    main{
		[ testParams()() {
			scope(test){
				install( default => 
					throw( TestFailed, "expected jot" )
				)
				equals@assertions({
					actual = p.name
					expected = "jot"
				})()
			}
		} ]
    }
}