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
            .v1 -> metadata.input.interfaces[ 0 ];
            .v2 -> metadata.input.interfaces[ 1 ]
        }
        undef( cmp.v1.name )
        undef( cmp.v2.name )
        compareValuesVectorLight@MetaJolie( cmp )( result )
        
    }

    delete@File( TESTFILE )(  )



}