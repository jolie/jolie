include "console.iol"
include "thermostat.iol"

outputPort Thermostat {
    Location: "datagram://localhost:5683"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .contentFormat = "text/plain",
            .alias = "%!{id}/getTemperature",
            .messageCode = "GET",
            .messageType = "CON"
        };
        .osc.setTmp << {
            .contentFormat = "text/plain",
            .alias = "%!{id}/setTemperature",
            .messageCode = "POST",
            .messageType = "CON"
        };
        .osc.core << {
            .contentFormat = "text/plain",
            .alias = "/.well-known/core",
            .messageCode = "GET",
            .messageType = "CON"
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    core@Thermostat( )( resp );
    println@Console( resp )();
    {
        println@Console( " Retrieving temperature 
        from Thermostat n.42 ... " )()
        |
        getTmp@Thermostat( { .id = "42" } )( t )
    };
    println@Console( " Thermostat n.42 forwarded temperature: " 
        + t + " C.")();
    t_confort = 21;
    if (t < t_confort) {
        println@Console( " Setting Temperature of Thermostat n.42 to " 
        + t_confort + " C ..." )()
        |
        setTmp@Thermostat( 21 { .id = "42" } );
        println@Console( " ... Thermostat set the Temperature 
        accordingly!" )()
    }
}
