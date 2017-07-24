include "console.iol"
include "iTmp.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.setTmp << {
            .format = "raw",
            .QoS = 1,
            .alias = "jolie/%!{id}/temperature"
        };
        .osc.getTmp << {
            .format = "raw",
            .QoS = 1,
            .alias = "jolie/%!{id}/temperature/request",
            .aliasResponse = "jolie/%!{id}/temperature/response"
        }                
    }
    Interfaces: ThermostatInterface
}

main
{
    //setTmp@Broker( 24 { .id = 42 } )
    getTmp@Broker( { .id = 42 } )( data )
}
