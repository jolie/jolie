include "iTmp.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.getTmp.alias = "jolie/get/temperature";
        .osc.setTmp.alias = "jolie/set/temperature"
    }
    Interfaces: ThermostatInterface
}

main 
{
    setTmp@Broker( "42" ) ; getTmp@Broker( "24" )
}
