include "console.iol"

inputPort  Server {
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
    setTmp( temp );
    println@Console( temp )()
}      