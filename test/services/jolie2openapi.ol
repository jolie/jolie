include "../AbstractTestUnit.iol"
include "services/openapi/public/interfaces/Jolie2OpenApiInterface.iol"
include "json_utils.iol"
include "console.iol"
include "string_utils.iol"
include "file.iol"
include "metajolie.iol"

constants {
    LOG = false
}

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

    template_json = "{ 
        \"getUsers\": {
            \"method\":\"post\", 
            \"template\":\"/users/{country}\"
        }, 
        \"getOrders\": {
            \"method\":\"get\", 
            \"template\":\"/orders/{userId}?maxItems={maxItems}\"
        },
        \"getOrdersByItem\": {
            \"method\":\"post\"
        },
        \"putOrder\": {
            \"method\":\"put\"
        },
        \"deleteOrder\": {
            \"method\":\"delete\"
        }}"
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

    f.filename = "./services/private/generated.json"
    f.content -> openapi_definition
    writeFile@File( f )(  )

    paths[ 0 ] = "/getOrdersByItem"
    paths[ 1 ] = "/putOrder"
    paths[ 2 ] = "/deleteOrder"
    paths[ 3 ] = "/orders/{userId}"
    paths[ 4 ] = "/users/{country}"

    for( i = 0, i < #paths, i++ ) {
        if ( !is_defined( json_value.paths.(paths[i]) ) ) {
            println@Console("Path not found:" + paths[i])()
            throw( TestFailed )
        }
    }

    undef( f )
    f.filename = "./services/private/DEMO.json"
    f.format = "json"
    readFile@File( f )( ok_json )


    scope( compare ) {
        install( ComparisonFailed =>
                println@Console("Error when checking generated value towards the ok value ")()
                throw( TestFailed )
        )
        compareValuesStrict@MetaJolie( { .v1 -> json_value, .v2 -> ok_json } )(  )
    }
    
    delete@File(  "./services/private/generated.json" )(  )


}