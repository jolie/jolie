include "console.iol"

inputPort  Server {
    Location: "datagram://localhost:9004"
    Protocol: coap
    OneWay: setTmp( string )
}

main 
{
    setTmp( temp );
    println@Console( temp )()
}       