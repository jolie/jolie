include "console.iol"

type TmpType: void

interface ThermostatInterfaceMQTT {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( int )
}

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .debug = true;
        .broker = "socket://localhost:1883";
        .osc.getTmp << {
            .format = "xml",
            .alias = "42/getTemperature",
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
        test( r );
        println@Console( "Received TEST: " + r )()
    }
    ;
    {
        getTmp( temp )( temp ){
            temp = 24;
            println@Console( "Received getTmp, sending back: " + temp )()
        }
    }
}