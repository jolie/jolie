include "console.iol"

interface AggregationInterfaceTest {
    RequestResponse: a, b
}


inputPort AggregationB {
    Location: "local"
    Interfaces: AggregationInterfaceTest
}

init {
    println@Console("Service B is running..." )()
}

main {
    a()( response ) {
        response = true
    }
}