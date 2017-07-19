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
            .alias = "jolie/request/temperature",
            .aliasResponse = "jolie/response/temperature"
        }                
    }
    Interfaces: ThermostatInterface
}

main 
{
    //setTmp@Broker( 24 { .id = 42 } )
    getTmp()( data ){
        println@Console( data )()
    }
}
