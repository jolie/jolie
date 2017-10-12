include "console.iol"

outputPort Server {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .osc.setTmp << {
            .format = "raw",
            .confirmable = true,
            .method = "POST"
        };
        .debug = true
    }
    OneWay: setTmp( int )
}

main
{
    setTmp@Server( 24 )
}
