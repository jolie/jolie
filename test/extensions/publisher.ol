include "iTmp.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.setTmp << {
            .format = "raw",
            .QoS = 1,
            .alias = "jolie/%{id}/setTemperature"
        };
        .osc.getTmp << {
            .format = "raw",
            .QoS = 1,
            .alias = "jolie/%!{id}/getTemperature",
            .aliasResponse = "jolie/%!{id}/getTempReply"
        }                
    }
    Interfaces: ThermostatInterface
}

main 
{
    setTmp@Broker( 24 { .id = 42 } )
    //getTmp@Broker( { .id = 42 } )( temp )
}
