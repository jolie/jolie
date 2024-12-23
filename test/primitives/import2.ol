from ..test-unit import TestUnitInterface

// this import does not exist and it should throw an error
from .a.b.c.d import TestInterface

interface TestInterface {
    RequestResponse:
        nothing
}

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

    main {
        test()() {
            nullProcess
        }
    }
}