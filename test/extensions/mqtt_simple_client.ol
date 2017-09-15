include "console.iol"

type TmpType: void { .id: string }

interface ThermostatInterfaceMQTT {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( int )
}

outputPort Broker {
    Location: "socket://localhost:1883"
    Protocol: mqtt {
        .debug = true;
        .osc.getTmp << {
            .format = "xml",
            .alias = "%!{id}/getTemperature",
            .QoS = 2
        };
        .osc.test << {
            .format = "xml",
            .alias = "test/getTemperature",
            .QoS = 2
        }
    }
    Interfaces: ThermostatInterfaceMQTT
}

main
{
    {
        test@Broker( "This is a test" );
        println@Console( "Test done" )()
    }
    ;
    {
        getTmp@Broker( { .id = "42" } )( varA );
        println@Console( "getTmp done: " + varA )()
    }
}
