include "console.iol"
include "thermostat.iol"

inputPort  Thermostat {
    Location: "datagram://localhost:9029"
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
            .alias = "/.well-known",
            .alias[1] = "/core",
            .messageCode = "205"
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
    // [
    //     getTmp( temp )( resp ){
    //         resp = 19;
    //         getTemperature            
    //     }
    // ]
    // |
    // [
    //     setTmp( temperatura )
    // ] 
    // {
    //     setTemperature
    // }
    // |
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