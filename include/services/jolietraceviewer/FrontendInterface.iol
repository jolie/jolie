type GetTraceListResponse: void {
    .trace*: string
}

type GetTraceLineRequest: void {
    .line: int
}

interface FrontendInterface {
    RequestResponse:
        getTraceList( void )( GetTraceListResponse ),
        getTraceLine( GetTraceLineRequest )( string ),
        getTrace( string )( string ),
        getServiceFile( string )( string )
}