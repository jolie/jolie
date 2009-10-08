include "TestUnit.iol"

execution { single }

inputPort TestUnitInput {
Location: "local"
Interfaces: TestUnitInterface
}

main
{
	test()() {
		doTest
	}
}