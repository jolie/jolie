include "console.iol"

interface ThermostatInterface { 
    OneWay: setTmp( string ), getTmp( string )
}

inputPort  Collector {
    Location: "socket://localhost:8050"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.getTmp.alias = "jolie/temperature";
        .osc.getTmp.format = "raw"
    }
    Interfaces: ThermostatInterface
}

//execution{ concurrent }

main 
{
    getTmp( data )
    //println@Console( data )()
}