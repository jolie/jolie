type GetTraceListResponse: void {
    .trace*: string
}

interface FrontendInterface {
    RequestResponse:
        getTraceList( void )( GetTraceListResponse ),
        getTrace( string )( string )
}