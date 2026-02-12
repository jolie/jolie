include "console.iol"

interface TestInterface {
	RequestResponse:
		testFlatStructure(undefined)(undefined)
}

outputPort Test {
	Location: "socket://localhost:9000"
	Protocol: sodep
	Interfaces: TestInterface
}

init {
	install( Err =>
		scope( pippo ) {
			with( request ) {
				..afield = "ciao";
				..bfield = 10;
				..cfield = 10.0;
				..efield = "ciao";
				..ffield = true;
				..gfield = "ciao";
				..hfield = 10L
				}
				request = "ciao";
				testFlatStructure@Test( request )( response )
			}
	)
}

main {
	throw( Err )
}
