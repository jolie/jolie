include "console.iol"

type TmpType: int | int { .id: string }

outputPort S_0 {
    Location: "datagram://localhost:8004/"
    Protocol: coap {
        .proxy = true
    }
    OneWay: setTmp( TmpType )
}

outputPort S_1 {
    Location: "datagram://localhost:8004/42/getTemperature"
    Protocol: coap
    OneWay: setTmp( TmpType )
}

outputPort S_2 {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .osc.setTmp << {
            .alias = "42/getTemperature"
        }
    }
    OneWay: setTmp( TmpType )
}

outputPort S_3 {
    Location: "datagram://localhost:8004"
    Protocol: coap {
        .osc.setTmp << {
            .alias = "%!{id}/getTemperature"
        }
    }
    OneWay: setTmp( TmpType )
}

main
{
    setTmp@S_0( 24 ) | setTmp@S_1( 24 ) | setTmp@S_2( 24 ) | setTmp@S_3( 24 { .id = "42" } )
}
