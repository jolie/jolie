include "console.iol"

type TmpType: void { .id: string } | int { .id: string }

interface ThermostatInterfaceMQTT {
    RequestResponse: getTmp( TmpType )( TmpType ), getTmp1( TmpType )( TmpType )
}

interface ThermostatInterface {
    RequestResponse: getTmp( TmpType )( int ), getTmp1( TmpType )( int )
}

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .debug = true;
        .broker = "socket://localhost:1883";
        .osc.getTmp << {
            .format = "raw",
            .alias = "42/getTemperature",
            .aliasResponse = "id",
            .QoS = 0
        };
        .osc.getTmp1 << {
            .format = "raw",
            .alias = "43/getTemperature",
            .aliasResponse = "id",
            .QoS = 0
        }
    }
    Interfaces: ThermostatInterfaceMQTT
}

execution{ concurrent }

main 
{
    [ getTmp( temp )( temp ){
        println@Console( "Received message with response topic: " + temp.id )();
        if( global.inputPorts.Thermostat.protocol != "mqtt" ) { undef( temp.id ) };
        temp = 24
    } ]
    [ getTmp1( temp1 )( temp1 ){
        println@Console( "Received message with response topic: " + temp1.id )();
        if( global.inputPorts.Thermostat.protocol != "mqtt" ) { undef( temp1.id ) };
        temp1 = 34
    } ]
}