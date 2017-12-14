include "console.iol"
include "thermostat.iol"

inputPort  Thermostat {
    Location: "datagram://localhost:5683"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .contentFormat = "text/plain",
            .messageCode = "CONTENT",
            .alias = "42/getTemperature"
        };
        .osc.setTmp << {
            .contentFormat = "text/plain",
            .alias = "42/setTemperature"
        };
        .osc.core << {
            .contentFormat = "text/plain",
            .messageCode = "205",
            .alias = "/.well-known/core"
        }
    }
    Interfaces: ThermostatInterface
}

main 
{
    [
        getTmp( temp )( resp ) 
        {
            resp = 19;
            println@Console( " Setting Temperature of the Thermostat to " 
                + temperatura )()   
        }
    ]
    |
    [
        setTmp( temperatura )
    ] 
    {
        println@Console( " Get Temperature Request. Forwarding: " 
            + resp + " C")()
    }
    |
    [
        core(  )( response )
        {
            response = 
            "
            </getTmp>;
                obs;
                rt=\"observe\";
                title=\"Resource for retrieving of the thermostat temperature\",
            </setTmp>;
                title=\"Resource for setting temperature\"
            "
        }
    ]
}      