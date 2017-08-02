include "console.iol"
include "iTmp.iol"

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.getTmp << {
            .format = "raw",
            .alias = "jolie/42/temperature/request"
        }
    }
    Interfaces: ThermostatInterface
}

execution{ concurrent }

main 
{
    getTmp( )( temp ) {
    }
}