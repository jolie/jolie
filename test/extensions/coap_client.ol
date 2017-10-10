include "console.iol"

outputPort Server {
    Location: "datagram://localhost:8001"
    Protocol: coap
    OneWay: setTmp( string )
}

main
{
    setTmp@Server( "24" )
}
