include "console.iol"

interface AggregationInterfaceTest {
    RequestResponse: a, b
}

outputPort AggregationB {
    Interfaces: AggregationInterfaceTest
}

embedded {
    Jolie:
    "private/aggregation_b.ol" in AggregationB
}

inputPort AggregationA {
    Location: "local"
    Interfaces: AggregationInterfaceTest
    Aggregates: AggregationB
}

init {
    println@Console("Service A is running..." )()
}

main {
    b()() {
        nullProcess
    }
}