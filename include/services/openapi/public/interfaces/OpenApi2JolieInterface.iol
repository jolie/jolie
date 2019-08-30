type GetJolieCodeForOpenApi2JolieClientRequest: void {
    .port_name: string
    .openapi: undefined
    .protocol: string
}

interface OpenApi2JolieInterface {
    RequestResponse:
        getJolieClient( GetJolieCodeForOpenApi2JolieClientRequest )( string )
}