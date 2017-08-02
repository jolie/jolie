include "console.iol"
include "iTmp.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.setTmp << {
            .alias = "jolie/%!{id}/temperature"
        };
        .osc.getTmp << {
            .alias = "jolie/%!{id}/temperature/request",
            .aliasResponse = "jolie/%!{id}/temperature/response"
        }                
    }
    Interfaces: ThermostatInterface
}

main
{
    getTmp@Broker( { .id = 42 } )( data )
}
