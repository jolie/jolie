include "iTmp.iol"
include "console.iol"

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
    //...
    getTmp(  )( temp ) {
        println@Console( "Temperature received " + temp )();
        temp = temp + 1;
        println@Console( "Temperature sent " + temp )()
    }
    //...
}