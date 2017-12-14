include "console.iol"
include "thermostat.iol"

inputPort  Thermostat {
    Location: "datagram://localhost:5683"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .format = "raw",
            .method = "205",
            .alias = "42/getTemperature"
        };
        .osc.setTmp << {
            .alias = "42/setTemperature"
        };
        .osc.core << {
            .alias = "/.well-known/core",
            .messageCode = "205"
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
            </obs>;
                obs;
                rt=\"observe\";
                title=\"Observable resource which changes every 5 seconds\",
            </obs-pumping>;
                obs;
                rt=\"observe\";
                title=\"Observable resource which changes every 5 seconds\"
            "
        }
    ]
}      