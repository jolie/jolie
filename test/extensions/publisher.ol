include "console.iol"

outputPort Broker {
    Location: "socket://test.mosquitto.org:1883"
    Protocol: mqtt
    RequestResponse: twice( int )( int )
}

main
{
    twice@Broker( 2 )( res );
    println@Console( res )()
}
