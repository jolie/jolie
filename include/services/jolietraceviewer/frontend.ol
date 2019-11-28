include "FrontendInterface.iol"
include "file.iol"

execution{ concurrent }

inputPort Frontend {
    Location: "local"
    Interfaces: FrontendInterface
}

main {
    [ getTrace( request )( response ) {
        f.filename = request
        readFile@File( f )( response )
    } ]

    [ getTraceList( request )( response ) {
        list@File( { .directory=".", .regex=".*\\.jolie\\.log\\.json"} )( list )
        traceCount = 0
        for( trace in list.result ) {
            response.trace[ traceCount ] = trace
            traceCount++
        }
    }]

    [ getServiceFile( request )( response ) {
        f.filename = request
        readFile@File( f )( response )
    }]
}

