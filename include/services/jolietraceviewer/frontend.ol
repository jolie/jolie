include "FrontendInterface.iol"
include "file.iol"
include "string_utils.iol"

execution{ concurrent }

inputPort Frontend {
    Location: "local"
    Interfaces: FrontendInterface
}

main {
    [ getTrace( request )( response ) {
        f.filename = request
        readFile@File( f )( response )
    } ] {
        split@StringUtils( response { .regex = "\\n" } )( global.trace_lines )
    }

    [ getTraceList( request )( response ) {
        list@File( { .directory=".", .regex=".*\\.jolie\\.log\\.json"} )( list )
        traceCount = 0
        for( trace in list.result ) {
            response.trace[ traceCount ] = trace
            traceCount++
        }
    }]

    [ getTraceLine( request )( response ) {
        response = global.trace_lines.result[ request.line ]
    }]

    [ getServiceFile( request )( response ) {
        f.filename = request
        readFile@File( f )( response )
    }]
}

