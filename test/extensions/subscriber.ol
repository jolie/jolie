include "console.iol"
include "iTmp.iol"

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .onDemand = false;
        .osc.getTmp << {
            .format = "raw",
            .alias = "jolie/request/temperature"
        }
    }
    Interfaces: ThermostatInterface
}

execution{ concurrent }

main 
{
    getTmp()( data ){
        println@Console( data )()
    }
}