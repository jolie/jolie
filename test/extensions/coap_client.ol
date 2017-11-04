include "console.iol"

type TmpType: void { .id?: string }

interface ThermostatInterface {
    OneWay: test( string )
    RequestResponse: getTmp( TmpType )( undefined )
}

outputPort Server {
    Location: "datagram://localhost:9000"
    Protocol: coap {
        .debug = true;
        .proxy = false;
        // .osc.getTmp << {
        //     .format = "raw",
        //     .alias = "%!{id}/getTemperature",
        //     .confirmable = true
        // };
        .osc.test << {
            .format = "raw",
            .alias = "test/getTemperature",
            .confirmable = true
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    {
        test@Server( "This is a test" );
        println@Console( "Test done" )()
    }
    // |
    // {
    //     getTmp@Server( { .id = "42" } )( varA );
    //     println@Console( "getTmp done: " + varA )()
    // }
}
