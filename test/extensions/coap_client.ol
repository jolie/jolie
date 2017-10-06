include "console.iol"

outputPort Server {
    Location: "datagram://localhost:9005"
    Protocol: coap
    OneWay: setTmp( string )
}

main
{
    setTmp@Server( "24" )
}
