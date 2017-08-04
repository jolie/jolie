inputPort  Thermostat {
    Location: "socket://localhost:9000"
    Protocol: mqtt {
        .broker = "socket://test.mosquitto.org:1883"
    }
    RequestResponse: twice( int )( int )
}

main 
{
    twice( x )( y ) {
        y = x + x
    }
}