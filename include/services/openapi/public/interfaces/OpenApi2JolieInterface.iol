type GetJolieCodeForOpenApi2JolieClientRequest: void {
    .port_name: string
    .openapi: undefined
    .protocol: string
}

type GetJolieCodeForOpenApi2JolieInterfacesRequest: void {
    .port_name: string
    .openapi: undefined
}

interface OpenApi2JolieInterface {
    RequestResponse:
        getJolieClient( GetJolieCodeForOpenApi2JolieClientRequest )( string ),
        getJolieInterface( GetJolieCodeForOpenApi2JolieInterfacesRequest )( string )
}