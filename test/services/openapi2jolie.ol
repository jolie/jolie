include "../AbstractTestUnit.iol"
include "services/openapi/public/interfaces/Jolie2OpenApiInterface.iol"
include "services/openapi/public/interfaces/OpenApi2JolieInterface.iol"
include "json_utils.iol"
include "file.iol"
include "runtime.iol"
include "console.iol"
include "string_utils.iol"
include "metajolie.iol"


outputPort Jolie2OpenApi {
    Interfaces: Jolie2OpenApiInterface
}

outputPort OpenApi2Jolie {
    Interfaces: OpenApi2JolieInterface
}

outputPort Test {
    RequestResponse: getOrders 
}

embedded {
    Jolie:
        "services/openapi/jolie2openapi.ol" in Jolie2OpenApi,
        "services/openapi/openapi2jolie.ol" in OpenApi2Jolie
}

constants {
    LOG = false
}

type CompareValuesRequest: bool | void {
    .__v1: undefined
    .__v2: undefined
}

interface ValuesUtilsInterface {
    RequestResponse: compareValues( CompareValuesRequest )( void ) throws ComparisonFailed( string )
}

service ValuesUtils {
    Interfaces: ValuesUtilsInterface
    init { install( ComparisonFailed => nullProcess )}
    main {
            [ compareValues( request )( response ) {

                // check root
                if ( request.__v1 != request.__v2 && !(request.__v1 == "SwaggerDemoOkInterface" || request.__v1 == "SwaggerDemoInterface")) {
                    throw( ComparisonFailed, "Different root values: " + request.__v1 + "<:::>" + request.__v2 )
                }

                // check if all the subnoeds of v1 exists in v2
                foreach( v : request.__v1 ) {
                    if ( LOG ) { println@Console("Navigating subnode " + v + ":" + request.__v1.( v ).name )() }
                    if ( is_defined( request.__v2.( v ) ) || ( request.__v1.( v ) instanceof void && request.__v2.( v ) instanceof void ) ) {
                        for( x = 0, x <#request.__v1.( v ), x++ ) {
                            with( cmp_rq ) {
                                .__v1 -> request.__v1.( v )[ x ];
                                found_item = false
                                for ( y = 0, y <#request.__v2.( v ), y++ ) {
                                    .__v2 -> request.__v2.( v )[ y ]
                                    scope( cmp_item ) {
                                        install( ComparisonFailed => nullProcess )
                                        compareValues@ValuesUtils( cmp_rq )( )
                                        found_item = true
                                    }

                                }
                                if ( !found_item ) { throw( ComparisonFailed , "Item " + x + " of node " + v + " whose value is " + request.__v1.( v )[ x ] + " does not have any correspondance")}
                            }
                            
                        }
                    } else {
                        throw( ComparisonFailed,  "Node " + v + " is not present in the target value" )
                    }
                }
            }]
    }
}

constants {
    SWAGGER_FILE = "./services/private/swagger.json",
    SWAGGER_OK = "./services/private/SwaggerDemoOkInterface.iol",
    TESTFILE = "./services/private/testswagger.ol"
}

define doTest {

    undef( f ) 
    f.filename = SWAGGER_FILE
    readFile@File( f )( openapi )
    getJsonValue@JsonUtils( openapi )( openapi_json )

    with( get_code_from_openapi ) {
        .port_name = "SwaggerDemo";
        .openapi -> openapi_json
    }
    getJolieInterface@OpenApi2Jolie( get_code_from_openapi )( interface_file )
    
    undef( f )
    f.filename = SWAGGER_OK
    readFile@File( f )( swagger_ok_interface )

    undef( f )
    f.content = interface_file + "\n\n" + swagger_ok_interface
    f.filename = TESTFILE
    writeFile@File( f )( )

     with( request_meta ) {
        .filename = TESTFILE
    }
    getInputPortMetaData@MetaJolie( request_meta )( metadata )

    scope( comparison ) {
        install( ComparisonFailed => 
            println@Console( comparison.ComparisonFailed )()
            throw(TestFailed, "Generated interface differs from what expected")
        )
        with( cmp ) {
            .__v1 -> metadata.input.interfaces[ 0 ];
            .__v2 -> metadata.input.interfaces[ 1 ]
        }
        compareValues@ValuesUtils( cmp )( result )
        with( cmp ) {
            .__v1 -> metadata.input.interfaces[ 1 ];
            .__v2 -> metadata.input.interfaces[ 0 ]
        }
        compareValues@ValuesUtils( cmp )()
    }

    delete@File( TESTFILE )(  )



}