include "console.iol"
include "iTmp.iol"

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.getTmp << {
            .format = "raw",
            .alias = "42/getTemperature"
        }
    }
    Interfaces: ThermostatInterface
}

main 
{
    getTmp()( 24 )
}