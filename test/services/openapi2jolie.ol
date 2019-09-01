include "../AbstractTestUnit.iol"
include "services/openapi/public/interfaces/Jolie2OpenApiInterface.iol"
include "services/openapi/public/interfaces/OpenApi2JolieInterface.iol"
include "json_utils.iol"
include "file.iol"
include "runtime.iol"
include "console.iol"
include "string_utils.iol"


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
    SOURCEFILE = "./services/private/testservice.ol",
    TESTFILE = "./services/private/testclient.ol",
    GENERATED_FILE = "./services/private/generated_json.json"
}

define doTest {

    service_filename = SOURCEFILE
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
    getJsonValue@JsonUtils( openapi_definition )( openapi )
    undef( f ) 
    f.filename = GENERATED_FILE
    f.content -> openapi_definition
    writeFile@File( f )(  )

    with( get_code_from_openapi ) {
        .port_name = service_input_port;
        .openapi -> openapi
    }
    getJolieInterface@OpenApi2Jolie( get_code_from_openapi )( interface_file )
    
    undef( f )
    f.filename = SOURCEFILE
    readFile@File( f )( source )
    undef( f )
    opdef = "outputPort " + service_input_port + "Port {\n Interfaces: " + service_input_port + "Interface }\n\n"
    f.content = interface_file + "\n\n" + opdef + source
    f.filename = TESTFILE
    writeFile@File( f )( )

    loadEmbeddedService@Runtime( { .filepath = TESTFILE, .type="Jolie" } )( Test.location )
    getOrders@Test( { .userId = "ciao", maxItems = 1 })()

    delete@File( TESTFILE )(  )
  //delete@File( GENERATE_FILE )(  )



}