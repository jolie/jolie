include "console.iol"

outputPort Server {
    Location: "datagram://localhost:9002"
    Protocol: http
    OneWay: setTmp( string )
}

main
{
    setTmp@Server( "24" )
}
