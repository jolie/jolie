type TmpType: void { .id: string } | int { .id: string }

interface ThermostatInterface {
  RequestResponse: getTmp( TmpType )( TmpType )
}

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://localhost:1883";
        .osc.getTmp << {
            .format = "raw",
            .alias = "42/getTemperature",
            .aliasResponse = "id",
            .QoS = 2
        };
        .debug = true
    }
    Interfaces: ThermostatInterface
}

main 
{
    getTmp( temp )( temp ){
        temp = 24
    }
}