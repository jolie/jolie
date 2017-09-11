include "console.iol"

type TmpType: void { .id: string } | int { .id: string }

interface ThermostatInterface {
  RequestResponse: getTmp( TmpType )( int ), getTmp1( TmpType )( int )
}

outputPort Broker {
    Location: "socket://localhost:1883"
    Protocol: mqtt {
        .osc.getTmp << {
            .QoS = 2,
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .aliasResponse = "%!{id}/getTempReply"
        };
        .debug = true
    }
    Interfaces: ThermostatInterface
}

main
{
    getTmp@Broker( { .id = "42" } )( varA );
    println@Console( "T1: " + varA )()
}
