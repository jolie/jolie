include "console.iol"

inputPort  Server {
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
    setTmp( temp );
    println@Console( temp )()
}      