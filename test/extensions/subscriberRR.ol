include "console.iol"

inputPort  Collector {
    Location: "socket://localhost:8050"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883";
        .osc.getTmp << {
            .alias = "temp/oW/random"
        };
        .osc.getTmpR << {
            .alias = "temp/rR/random"
        }
    }
    Interfaces: Itemp
}

execution{ concurrent }

main {
    getTmp( dataOW ) | getTmpR( "jolie/temp/response" )( dataRR );
    println@Console( "One Way temperature is: " + dataOW + "Request Response temperature is: "  + dataRR )()
}
