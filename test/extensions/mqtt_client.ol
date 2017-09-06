include "console.iol"

type TmpType: void { .id: string } | int { .id: string }

interface ThermostatInterface {
  RequestResponse: getTmp( TmpType )( int )
}

outputPort Broker {
    Location: "socket://localhost:1883"
    Protocol: mqtt {
        .osc.getTmp << {
            .QoS = 2,
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .aliasResponse = "%!{id}/getTempReply"
        };
        .debug = true
    }
    Interfaces: ThermostatInterface
}

main
{
  {
    getTmp@Broker( { .id = "43" } )( t2 )
    |
    getTmp@Broker( { .id = "42"} )( t1 )
  };
  println@Console( "T1: " + t1 )();
  println@Console( "T2: " + t2 )()
}
