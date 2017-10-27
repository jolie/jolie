include "console.iol"

inputPort  S {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .debug = true;
        .proxy = false
    }
    OneWay: getTemperature( int )
}

execution{ concurrent }

main 
{
    getTemperature( temp );
    println@Console( temp )()
}      