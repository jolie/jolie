from ...tools.jolie2openapi.jolie2openapi import Jolie2OpenApi
from ..test-unit import TestUnitInterface


service Main {
	
    embed Jolie2OpenApi as Jolie2OpenApi
	
    inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	

	main {
		[ test()() {
            nullProcess 
        }]
    }
}

