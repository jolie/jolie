include "console.iol"

type TmpType: void { .id?: string } | int { .id?: string }

interface ThermostatInterface {
    OneWay: setTmp( TmpType )
    RequestResponse: getTmp( TmpType )( int )
}

inputPort  Thermostat {
    Location: "datagram://localhost:9020"
    Protocol: coap {
        .debug = true;
        .proxy = false;
        .osc.getTmp << {
            .format = "raw",
            .method = "CONTENT_205",
            .alias = "42/getTemperature"
        };
        .osc.setTmp << {
            .alias = "42/setTemperature"
        }
    }
    Interfaces: ThermostatInterface
}

execution{ concurrent }

main 
{
    [ getTmp( temp )( resp ){
            resp = 19;
            println@Console( " Get Temperature Request. Forwarding: " + resp )()
        } 
    ]
    [ setTmp( r ) ]{
        println@Console( " Setting Temperature of the Thermostat to " + r )()
    }
}      