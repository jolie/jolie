include "console.iol"

inputPort  Server {
    Location: "datagram://localhost:8001"
    Protocol: coap
    OneWay: setTmp( string )
}

main 
{
    setTmp( temp );
    println@Console( temp )()
}       