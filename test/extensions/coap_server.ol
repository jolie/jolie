include "console.iol"

inputPort  Server {
    Location: "datagram://localhost:9002"
    Protocol: sodep
    OneWay: setTmp( string )
}

main 
{
    setTmp( temp );
    println@Console( temp )()
}