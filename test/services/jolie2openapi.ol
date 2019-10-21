include "../AbstractTestUnit.iol"
include "services/openapi/public/interfaces/Jolie2OpenApiInterface.iol"
include "json_utils.iol"
include "console.iol"
include "string_utils.iol"
include "file.iol"

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

type CompareValuesRequest: bool | void {
    .__v1: undefined
    .__v2: undefined
}

interface ValuesUtilsInterface {
    RequestResponse: compareValues( CompareValuesRequest )( void ) throws ComparisonFailed
}

service ValuesUtils {
    Interfaces: ValuesUtilsInterface
    main {
            [ compareValues( request )( response ) {

                // check root
                if ( request.__v1 != request.__v2 ) {
                    println@Console("Different root values: " + request.__v1 + "<:::>" + request.__v2 )()
                    throw( ComparisonFailed )
                }

                // check if all the subnoeds of v1 exists in v2
                foreach( v : request.__v1 ) {
                    if ( LOG ) { println@Console("Navigating subnode " + v )() }
                    if ( is_defined( request.__v2.( v ) ) || ( request.__v1.( v ) instanceof void && request.__v2.( v ) instanceof void ) ) {
                        for( x = 0, x <#request.__v1.( v ), x++ ) {
                            with( cmp_rq ) {
                                .__v1 -> request.__v1.( v )[ x ];
                                .__v2 -> request.__v2.( v )[ x ]
                            }
                            compareValues@ValuesUtils( cmp_rq )( response )
                        }
                    } else {
                        println@Console( "Node " + v + " is not present in the target value")()
                        throw( ComparisonFailed )
                    }
                }
            }]
    }
}

define doTest {
    service_filename = "./services/private/testservice.ol"
    service_input_port = "DEMO"
    router_host = "localhost:8000"
    wkdir = "./services/private"

    template_json = "{ \"getUsers\":\"method=post, template=/users/{country}\", \"getOrders\":\"method=get, template=/orders/{userId}?maxItems={maxItems}\",\n\"getOrdersByItem\":\"method=post\",\n\"putOrder\":\"method=put\",\n\"deleteOrder\":\"method=delete\"}"
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
    f.format = "json"
    f.content -> json_value
    writeFile@File( f )(  )

    paths[ 0 ] = "/getOrdersByItem"
    paths[ 1 ] = "/putOrder"
    paths[ 2 ] = "/deleteOrder"
    paths[ 3 ] = "/orders/{userId}?maxItems={maxItems}"
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
        compareValues@ValuesUtils( { .__v1 -> json_value, .__v2 -> ok_json } )(  )
    }
    
    scope( compare ) {
        install( ComparisonFailed =>
                println@Console("Error when checking ok value towards the generated value ")()
                throw( TestFailed )
        )
        compareValues@ValuesUtils( { .__v2 -> json_value, .__v1 -> ok_json } )(  )
    }
    delete@File(  "./services/private/generated.json" )(  )


}