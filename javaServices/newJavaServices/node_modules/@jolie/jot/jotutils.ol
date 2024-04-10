type FindTestOperationsResponse {
	services* {
		name:string
		beforeAll*:string
		afterAll*:string
		beforeEach*:string
		afterEach*:string
		tests*:string
	}
}

interface JotUtilsInterface {
RequestResponse:
	findTestOperations(string)(FindTestOperationsResponse)
}

service JotUtils {
	inputPort Input {
		location: "local"
		interfaces: JotUtilsInterface
	}
	foreign java {
		class: "jolie.jot.JotUtils"
	}
}