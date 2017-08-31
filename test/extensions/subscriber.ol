include "iTmp.iol"

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.getTmp << {
            .format = "raw",
            .alias = "42/getTemperature",
            .aliasResponse = "id"
        }
    }
    Interfaces: ThermostatInterface
}

execution{ concurrent }

main 
{
    getTmp( )( "24" )
}