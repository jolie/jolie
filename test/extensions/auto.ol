include "../AbstractTestUnit.iol"

interface DummyInterface {
OneWay: dummy(string)
}

inputPort MyInput {
location: "auto:ini:/Test/Location:file:extensions/private/auto.ini"
protocol: sodep
interfaces: DummyInterface
}

inputPort MyInput2 {
location: "auto:json:Test.location:file:extensions/private/auto.json"
protocol: sodep
interfaces: DummyInterface
}

outputPort MyOutput2 {
location: "auto:json:Test.location:file:extensions/private/auto.json"
protocol: sodep
interfaces: DummyInterface
}

define doTest
{
	if ( global.inputPorts.MyInput.location != "socket://localhost:8000/" ) {
		throw( TestFailed, "auto fetched the wrong location: " + global.inputPorts.MyInput.location )
	}

	if ( global.inputPorts.MyInput2.location != "socket://localhost:8001/" ) {
		throw( TestFailed, "auto fetched the wrong location: " + global.inputPorts.MyInput2.location )
	}

	{ dummy( x ) | dummy@MyOutput2( "ping" ) }
	if ( x != "ping" ) {
		throw ( TestFailed, "auto fetched different configurations for input and output ports" )
	}
}
