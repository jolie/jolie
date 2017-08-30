type t: string { .id: string} | string

inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.twice << {
            .format = "raw",
            .alias = "42/twice",
            .aliasResponse = "id"
        }
    }
    RequestResponse: twice( t )( t )
}

main 
{
    twice( one )( two ) {
        two = one + "ciao"
    }
}