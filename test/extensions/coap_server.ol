include "console.iol"

inputPort  Server {
    Location: "datagram://localhost:9002"
    Protocol: http
    OneWay: setTmp( string )
}

main 
{
    setTmp( temp );
    println@Console( temp )()
}