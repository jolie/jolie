include "console.iol"

type t: string { .id: string} | string

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt {
        .osc.twice << {
            .QoS = 1,
            .format = "raw",
            .alias = "42/twice",
            .aliasResponse = "42/twice/response"
        }
    }
    RequestResponse: twice( t )( t )
}

main
{
    twice@Broker( "2" )( temp );
    println@Console( temp )()
}
