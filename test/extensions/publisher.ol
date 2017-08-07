include "console.iol"
include "iTmp.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.getTmp << {
            .format = "raw",
            .QoS = 2,
            .alias = "%!{id}/getTemperature",
            .aliasResponse = "%!{id}/getTempReply"
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    // ...
    getTmp@Broker( { .id = 42 } )( temp );
    println@Console( temp )()
    // ...
}
