inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt
    RequestResponse: twice( int )( int )
}

main 
{
    twice( x )( y ) {
        y = x + x
    }
}