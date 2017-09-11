include "console.iol"

type TmpType: int //| void

interface ThermostatInterfaceMQTT {
    RequestResponse: getTmp( TmpType )( int )
}

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .debug = true;
        .broker = "socket://localhost:1883";
        .osc.getTmp << {
            .format = "raw",
            .alias = "42/getTemperature",
            .QoS = 2
        }
    }
    Interfaces: ThermostatInterfaceMQTT
}

main 
{
    getTmp( temp )( temp ){
        temp = 24
    }
}