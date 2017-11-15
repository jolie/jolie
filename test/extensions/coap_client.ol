include "console.iol"

type TmpType: void { .id?: string }

interface ThermostatInterface {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( int )
}

outputPort Server {
    Location: "datagram://localhost:9020"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .method = "GET",
            .confirmable = true
        };
        .osc.test << {
            .format = "raw",
            .method = "POST",
            .alias = "test/getTemperature",
            .confirmable = true
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    // {
    //     test@Server( "This is a test" );
    //     println@Console( "Test done" )()
    // } 
    // |
    {
        getTmp@Server( { .id = "42" } )( varA );
        println@Console( "getTmp done: " + varA )()
    }
}
