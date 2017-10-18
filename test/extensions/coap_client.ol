include "console.iol"

outputPort Server {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .osc.setTmp << {
            .format = "raw",
            .confirmable = false,
            .method = "POST"
        };
        .debug = true
    }
    OneWay: setTmp( int )
}

/*
outputPort Server {
    Location: "socket://localhost:8004"
    Protocol: http {
        .debug = true
    }
    OneWay: setTmp( int )
}
*/

main
{
    setTmp@Server( 24 )
}
