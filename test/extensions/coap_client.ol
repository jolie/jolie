include "console.iol"

type TmpType: void { .id?: string } | int { .id?: string }

interface ThermostatInterface {
    OneWay: setTmp( TmpType )
    RequestResponse: getTmp( TmpType )( int )
}

outputPort Thermostat {
    Location: "datagram://localhost:9027"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .alias = "%!{id}/getTemperature",
            .method = "GET",
            .confirmable = true
        };
        .osc.setTmp << {
            .format = "raw",
            .method = "POST",
            .alias = "%!{id}/setTemperature",
            .confirmable = true
        }
    }
    Interfaces: ThermostatInterface
}

main
{
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
