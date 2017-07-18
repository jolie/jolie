include "console.iol"
include "iTmp.iol"

inputPort  Collector {
    Location: "socket://localhost:8050"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.getTmp.alias = "jolie/get/temperature";
        .osc.setTmp.alias = "jolie/set/temperature"
    }
    Interfaces: ThermostatInterface
}

execution{ concurrent }

main 
{
    getTmp( dataG ); 
    setTmp( dataS );
    println@Console( "Temperature from getTmp: " + dataG )();
    println@Console( "Temperature from setTmp: " + dataS )()
}