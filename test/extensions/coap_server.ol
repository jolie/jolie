include "console.iol"

type TmpType: void { .id?: string }

interface ThermostatInterface {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( int )
}

inputPort  Thermostat {
    Location: "datagram://localhost:9001"
    Protocol: coap {
        .debug = true;
        .proxy = false;
        // .osc.getTmp << {
        //     .format = "raw",
        //     .alias = "42/getTemperature",
        //     .confirmable = true
        // };
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
    // [ getTmp( temp )( resp ){
    //         resp = 24;
    //         println@Console( "Received getTmp, sending back: " + resp )()
    //     } 
    // ]
}      