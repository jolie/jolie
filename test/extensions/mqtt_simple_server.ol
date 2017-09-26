include "console.iol"

type TmpType: void { .id?: string }

interface ThermostatInterface {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( int )
}

inputPort Thermostat2 {
    Location: "socket://localhost:8000"
    Protocol: sodep
    Interfaces: ThermostatInterface
}

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .debug = true;
        .broker = "socket://localhost:1883";
        .osc.getTmp << {
            .format = "raw",
            .alias = "42/getTemperature",
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

execution{ concurrent }

main 
{
    [ test( r ) ]{
        println@Console( "Received TEST: " + r )()
    }
    [ getTmp( temp )( resp ){
            resp = 24;
            println@Console( "Received getTmp, sending back: " + resp )()
        } 
    ]
}