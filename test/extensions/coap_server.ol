include "console.iol"

inputPort  S {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .debug = true
    }
    OneWay: getTemperature( int )
}

execution{ concurrent }

main 
{
    getTemperature( temp );
    println@Console( temp )()
}      