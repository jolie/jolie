
include "console.iol"

type TmpType: void { .id?: string }

interface ThermostatInterface {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( undefined )
}

outputPort Server {
    Location: "socket://localhost:8000"
    Protocol: sodep
    Interfaces: ThermostatInterface
}

outputPort Broker {
    Location: "socket://localhost:1883"
    Protocol: mqtt {
        .debug = true;
        .osc.getTmp << {
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .QoS = 2
        };
        .osc.test << {
            .format = "raw",
            .alias = "test/getTemperature",
            .QoS = 2
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    {
        test@Broker( "This is a test" );
        println@Console( "Test done" )()
    }
    |
    {
        getTmp@Server( { .id = "42" } )( varA );
        println@Console( "getTmp done: " + varA )()
    }
}
