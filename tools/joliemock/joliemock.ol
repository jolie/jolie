
include "services/joliemock/public/interfaces/JolieMockInterface.iol"
include "metajolie.iol"
include "console.iol"

outputPort JolieMock {
    Interfaces: JolieMockInterface
}


embedded {
    Jolie:
        "services/joliemock/joliemock.ol" in JolieMock
}

main {
    if ( #args == 0 ) {
        println@Console("Usage: joliemock <filename> [-port <portname>] [-depth <vector depth>]")()
        println@Console("By default the first port is converted")()
        println@Console("By default vector depth = 5")()
    } else {
        getInputPortMetaData@MetaJolie( { .filename = args [ 0 ] } )( ipts )
        rq.vector_depth = 5
        port_name = Void
        if ( #args > 1 ) {
            for ( a = 0, a < #args, a++ ) {
                if ( args[ a ] == "-port" ) {
                    port_name = args[ a + 1 ]
                }
                if ( args[ a ] == "-depth" ) {
                    rq.vector_depth = int( args[ a + 1 ] )
                }
            }
        }
        if ( port_name != Void ) {
            for( ip in ipts.input ) {
                if ( ip.name == port_name ) {
                    rq.input -> ip
                }
            }
        } else {
            rq.input -> ipts.input 
        }
        getMock@JolieMock( rq )( mock )
        println@Console( mock )()
    }
}