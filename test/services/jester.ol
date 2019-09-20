include "../AbstractTestUnit.iol"
include "services/openapi/public/interfaces/Jolie2OpenApiInterface.iol"
include "services/openapi/public/interfaces/OpenApi2JolieInterface.iol"
include "services/jester/router.iol"
include "json_utils.iol"
include "file.iol"
include "runtime.iol"
include "console.iol"
include "string_utils.iol"
include "metajolie.iol"
include "metarender.iol"
include "services/jester/JesterConfiguratorInterface.iol"


outputPort JesterConfigurator {
    Interfaces: JesterConfiguratorInterface
}


outputPort Jolie2OpenApi {
    Interfaces: Jolie2OpenApiInterface
}

outputPort OpenApi2Jolie {
    Interfaces: OpenApi2JolieInterface
}

outputPort Jester {
    Interfaces: RouterIface
}


outputPort Test {
    RequestResponse: 
    getOrders,
    getOrdersByItem throws FaultTest( undefined ),
    putOrder,
    deleteOrder,
    getUsers
}

embedded {
    Jolie:
        "services/openapi/jolie2openapi.ol" in Jolie2OpenApi,
        "services/openapi/openapi2jolie.ol" in OpenApi2Jolie,
        "services/jester/jester_configurator.ol" in JesterConfigurator
}

constants {
    SOURCEFILE = "./services/private/testservice.ol",
    TESTFILE = "./services/private/testclient.ol"
}

define doTest {

    service_filename = SOURCEFILE
    service_input_port = "DEMO"
    router_host = "localhost:8000"
    wkdir = "./services/private"
    protocol = "http"

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
    getJsonValue@JsonUtils( openapi_definition )( openapi )

    with( request_meta ) {
        .filename = service_filename
    }
    getInputPortMetaData@MetaJolie( request_meta )( metadata )
    for( i = 0, i < #metadata.input, i++ ) {
        // port selection from metadata
        if ( metadata.input[ i ].name == service_input_port ) {
              getSurface@MetaRender( metadata.input )( surface )
              getSurfaceWithoutOutputPort@MetaRender( metadata.input )( surface_wop )
        }
    }

    undef( f )
    f.filename = "./services/private/DEMOInterface.iol"
    f.content -> surface_wop
    writeFile@File( f )(  )
    
    with( get_code_from_openapi ) {
        .port_name = service_input_port;
        .openapi -> openapi;
        get_code_from_openapi.protocol = protocol
    }
    getJolieClient@OpenApi2Jolie( get_code_from_openapi )( client_file )

    undef( f )
    f.content = client_file
    f.filename = TESTFILE
    writeFile@File( f )( )

    // embedding client
    loadEmbeddedService@Runtime( { .filepath = TESTFILE, .type="Jolie" } )( Test.location )

    // running jester
    with( jester ) {
        .filename = service_filename;
        .host = router_host;
        .inputPort = service_input_port;
        .easyInterface = false;
        if ( !easy_interface ) {
            .template -> template
        }
    }

    
    undef( f )
    f.content = surface + "\nembedded { Jolie: \"" + service_filename + "\" in " + service_input_port + " }\n"
    f.filename = "jester_config.iol"
    writeFile@File( f )()
    
    getJesterConfig@JesterConfigurator( jester )( config );

    loadEmbeddedService@Runtime( { .filepath = "-C DEBUG=false -C API_ROUTER_HTTP=\"socket://" + router_host + "\" services/jester/router.ol", .type="Jolie"} )( Jester.location )
    config@Jester( config )()

    // testing jester invoking the client
    with( rq ) {
        .userId = "ciao";
        .maxItems = 10
    }
    getOrders@Test( rq )()

    undef( rq )
    with( rq ) {
      .userId = "ciao"
      with( .order ) {
        .title = "title";
        .id = 5;
        .date = "date"
      }
    }
    putOrder@Test( rq )()

    undef( rq )
    with( rq ) {
        .orderId = 10
    }
    deleteOrder@Test( rq )()

    undef( rq )
    with( rq ) {
        .city = "Springfield";
        .country = "USA";
        .surname = "White"
    }
    getUsers@Test( rq )( rs )
    if ( #rs.users != 2 ) {
        throw( TestFailed, "Expected 2 users in getUsers operation")
    }

    undef( rq ) 
    with( rq ) {
        .userId = "ciao";
        .itemName= "ciao";
        .quantity = 0
    }
    getOrdersByItem@Test( rq )()

   
    undef( rq )
    with( rq ) {
        .userId = "ciao";
        .itemName= "ciao"
    }
    getOrdersByItem@Test( rq )()
    undef( rq )
    with( rq ) {
        .userId = "ciao"
    }
    getOrdersByItem@Test( rq )()

    undef( rq )
    scope( fault ) {
        install( FaultTest => nullProcess )
        with( rq ) {
            .userId = "ciao";
            .itemName= "ciao";
            .quantity = 10
        }
        getOrdersByItem@Test( rq )()
    }


    delete@File( TESTFILE )(  )
    delete@File( "jester_config.iol" )()
    delete@File("services/private/DEMOInterface.iol")()

}