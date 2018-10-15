include "../AbstractTestUnit.iol"

interface DummyInterface {
OneWay: dummy(string)
}

inputPort MyInput {
Location: "auto:ini:/Test/Location:file:extensions/private/auto.ini"
Protocol: sodep
Interfaces: DummyInterface
}

define doTest
{
	if ( global.inputPorts.MyInput.location != "socket://localhost:8000/" ) {
		throw( TestFailed, "auto fetched the wrong location: " + global.inputPorts.MyInput.location )
	}
}
