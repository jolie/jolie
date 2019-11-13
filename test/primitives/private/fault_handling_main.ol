include "fault_handling_main.iol"

execution { single }

inputPort Input {
Location: "local"
Interfaces: FaultHandlingMainIface
}

main {
	install( Err =>
		reply(x)(x)
	)
	throw( Err )
}