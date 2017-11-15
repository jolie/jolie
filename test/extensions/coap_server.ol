include "console.iol"

type TmpType: void { .id?: string }

interface ThermostatInterface {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( int )
}

inputPort  Thermostat {
    Location: "datagram://localhost:9020"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .format = "raw",
            .method = "POST",
            .alias = "42/getTemperature"
        };
        .osc.test << {
            .format = "raw",
            .alias = "test/getTemperature"
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