type TmpType: void { .id: string } | int { .id: string }

interface ThermostatInterface {
  RequestResponse: getTmp( TmpType )( TmpType )
}

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.getTmp << {
            .format = "json",
            .alias = "42/getTemperature",
            .aliasResponse = "id"
        }
    }
    Interfaces: ThermostatInterface
}

main 
{
    getTmp( temp )( temp ){
        temp = 24
    }
}