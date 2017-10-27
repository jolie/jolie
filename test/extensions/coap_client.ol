include "console.iol"

type TmpType: int | int { .id: string }

outputPort S {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .proxy = false;
        .debug = true;
        .osc.setTmp << {
            .alias = "%!{id}/getTemperature"
        }
    }
    OneWay: setTmp( TmpType )
}

main
{
    setTmp@S( 24 { .id = "42" } )
}
