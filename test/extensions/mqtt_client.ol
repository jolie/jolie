include "console.iol"

type TmpType: void /*{ .id: string }*/ | int /*{ .id: string }*/

interface ThermostatInterface {
  RequestResponse: getTmp( TmpType )( int )
}

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.getTmp << {
            .QoS = 2,
            .format = "json",
            .alias = "%!{id}/getTemperature",
            .aliasResponse = "%!{id}/getTempReply"
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    getTmp@Broker( /*{ .id = "42"}*/ )( temp ); 
    println@Console( temp )()
}
