include "console.iol"

type TmpType: int | int { .id: string }

outputPort S_1 {
    Location: "datagram://localhost:8004/42/getTemperature"
    Protocol: coap {
        .osc.setTmp << {
            .format = "raw"
        };
        .debug = true
    }
    OneWay: setTmp( TmpType )
}

outputPort S_2 {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .osc.setTmp << {
            .format = "raw",
            .alias = "42/getTemperature"
        };
        .debug = true
    }
    OneWay: setTmp( TmpType )
}

outputPort S_3 {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .osc.setTmp << {
            .format = "raw",
            .alias = "%!{id}/getTemperature"
        };
        .debug = true
    }
    OneWay: setTmp( TmpType )
}

main
{
    setTmp@S_1( 24 ) | setTmp@S_2( 24 ) | setTmp@S_3( 24 { .id = "42" } )
}
