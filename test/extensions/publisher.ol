interface ThermostatInterface { 
    OneWay: setTmp( int )
}

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.setTmp << {
            .alias = "%!{id}/setTemperature",
            .format = "raw",
            .QoS = 1
        }
    }
    Interfaces: ThermostatInterface
}

main 
{
    setTmp@Broker( 24 { .id = 42 } )
}
