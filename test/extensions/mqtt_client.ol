include "console.iol"

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
        };
        .debug = true
    }
    Interfaces: ThermostatInterface
}

main
{
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