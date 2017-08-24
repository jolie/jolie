include "console.iol"
include "iTmp.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.getTmp << {
            .QoS = 2,
            .format = "raw",
            .alias = "42/getTemperature",
            .aliasResponse = "42/getTempReply"
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    getTmp@Broker( )( temp );
    println@Console( temp )()
}
