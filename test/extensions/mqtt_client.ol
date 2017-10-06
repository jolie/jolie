include "console.iol"

type TmpType: void { .id: string } | int { .id: string }

interface ThermostatInterface {
  RequestResponse: getTmp( TmpType )( int ), getTmp1( TmpType )( int )
}

outputPort Broker {
    Location: "socket://localhost:1883"
    Protocol: mqtt {
        .osc.getTmp << {
            .QoS = 0,
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .aliasResponse = "%!{id}/getTempReply"
        };
        .osc.getTmp1 << {
            .QoS = 0,
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .aliasResponse = "%!{id}/getTempReply"
        }
        ;.debug = true
    }
    Interfaces: ThermostatInterface
}

main
{
  for ( i=0, i<3, i++ ) {
    {
      getTmp@Broker( { .id = "42" } )( varA );
      println@Console( "T1: " + varA )()
    }
    |
    {
      getTmp1@Broker( { .id = "43" } )( varB );
      println@Console( "T2: " + varB )()
    }  
  }
}