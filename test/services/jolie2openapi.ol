include "../AbstractTestUnit.iol"
include "services/openapi/public/interfaces/Jolie2OpenApiInterface.iol"
include "json_utils.iol"
include "console.iol"

outputPort Jolie2OpenApi {
    Interfaces: Jolie2OpenApiInterface
}

embedded {
    Jolie:
        "services/openapi/jolie2openapi.ol" in Jolie2OpenApi
}

define doTest {
    service_filename = "./services/private/testservice.ol"
    service_input_port = "DEMO"
    router_host = "localhost:8000"
    wkdir = "./services/private"

    template_json = "{ \"getOrders\":\"method=get,\ntemplate=/orders/{userId}?maxItems={maxItems}\",\n\"getOrdersByIItem\":\"method=post\",\n\"putOrder\":\"method=put\",\n\"deleteOrder\":\"method=delete\"}"
    getJsonValue@JsonUtils( template_json )( template )
    with( openapi ) {
        .filename = service_filename;
        .host = router_host;
        .inputPort = service_input_port;
        .easyInterface = false;
        if ( !easy_interface ) {
            .template -> template
        }
    }

    getOpenApi@Jolie2OpenApi( openapi )( openapi_definition )
    getJsonValue@JsonUtils( openapi_definition )( json_value )

    paths[ 0 ] = "/getOrdersByIItem"
    paths[ 1 ] = "/putOrder"
    paths[ 2 ] = "/deleteOrder"
    paths[ 3 ] = "/orders/{userId}?maxItems={maxItems}"

    for( i = 0, i < #paths, i++ ) {
        if ( !is_defined( json_value.paths.(paths[i]) ) ) {
            println@Console("Path not found:" + paths[i])()
            throw( TestFailed )
        }
    }
}