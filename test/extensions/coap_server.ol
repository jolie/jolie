include "console.iol"

type TmpType: void { .id?: string } | int { .id?: string }

interface ThermostatInterface {
    OneWay: setTmp( TmpType )
    RequestResponse: getTmp( TmpType )( int )
}

inputPort  Thermostat {
    Location: "datagram://localhost:9028"
    Protocol: coap {
        .debug = true;
        .proxy = false;
        .osc.getTmp << {
            .format = "raw",
            .method = "205",
            .alias = "42/getTemperature"
        };
        .osc.setTmp << {
            .alias = "42/setTemperature"
        }
    }
    Interfaces: ThermostatInterface
}

execution{ concurrent }

define setTemperature
{
  println@Console( " Setting Temperature of the Thermostat to " + temperatura )()
}

define getTemperature
{
  println@Console( " Get Temperature Request. Forwarding: " + resp + " C")()
}

main 
{
    [
        getTmp( temp )( resp ){
            resp = 19;
            getTemperature            
        }
    ]
    [
        setTmp( temperatura )
    ] 
    {
        setTemperature
    }
}      