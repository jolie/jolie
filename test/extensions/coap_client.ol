include "console.iol"

outputPort Server {
    Location: "datagram://localhost:8003"
    Protocol: coap {
        .osc.setTmp << {
            .format = "raw"
        };
        .debug = true
    }
    OneWay: setTmp( int )
}

main
{
    setTmp@Server( 24 )
}
