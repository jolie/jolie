include "iTmp.iol"
include "console.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.getTmp << {
            .QoS = 1,
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .aliasResponse = "%!{id}/getTempReply"
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    getTmp@Broker( { .id= "42"} )( temp );  
    println@Console( temp )()
}
