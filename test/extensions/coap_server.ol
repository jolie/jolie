include "console.iol"

inputPort  Server {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .debug = true
    }
    OneWay: setTmp( int )
}

main 
{
    setTmp( temp );
    println@Console( temp )()
}      