include "console.iol"

type TmpType: void { .id: string } | int { .id: string }

interface ThermostatInterface {
  RequestResponse: getTmp( TmpType )( TmpType ), getTmp1( TmpType )( TmpType )
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
        .osc.getTmp1 << {
            .format = "raw",
            .alias = "43/getTemperature",
            .aliasResponse = "id",
            .QoS = 2
        };
        .debug = true
    }
    Interfaces: ThermostatInterface
}

execution{ concurrent }

main 
{
    [ 
    getTmp( temp )( temp ){
        println@Console( "Received message with response topic: " + temp.id )();
        temp = 24
    }]{ println@Console( "Finished computation for 42/getTemperature" )()}
    [ getTmp1( temp )( temp ){
        println@Console( "Received message with response topic: " + temp.id )();
        temp = 34
    }]{ println@Console( "Finished computation for 43/getTemperature" )()}
}