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

    template_json = "{ 
        \"getUsers\":{
            \"method\":\"post\", 
            \"template\":\"users/{country}\"
        }, 
        \"getOrders\": {
            \"method\":\"get\", 
            \"template\":\"orders/{userId}?maxItems={maxItems}\"
        },
        \"getOrdersByItem\": {
            \"method\":\"post\"
        },
        \"putOrder\": {
            \"method\":\"put\"
        },
        \"deleteOrder\": {
            \"method\":\"delete\"
        }
        }"
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
    with( gji ) {
        .port_name = service_input_port;
        .openapi -> openapi
    };
    getJolieInterface@OpenApi2Jolie( gji )( client_interface )
    

    undef( f )
    f.filename = "./services/private/DEMOInterface.iol"
    f.content -> client_interface
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

    getInputPortMetaData@MetaJolie({ filename = SOURCEFILE } )( testservice_ip_meta )
    getSurfaceWithoutOutputPort@MetaRender( testservice_ip_meta.input )( testservice_surface )
    
    undef( f )
    f.content = testservice_surface + "outputPort DEMO {\nInterfaces: DEMOInterface\n}\n\nembedded { Jolie: \"" + service_filename + "\" in " + service_input_port + " }\n"
    f.filename = "jester_config.iol"
    writeFile@File( f )()
    
    getJesterConfig@JesterConfigurator( jester )( config );

    loadEmbeddedService@Runtime( { .filepath = "-C DEBUG=false" +
                                                " -C API_ROUTER_HTTP=\"socket://" + router_host  + 
                                                "\" -C API_ROUTER_HTTPS=\"local"  +
                                                "\" -C KEY_STORE=\"" + jester_https_keyStore +
                                                "\" -C KEY_STORE_PASSWORD=\"" + jester_https_keyStorePassword +
                                                "\" -C TRUST_STORE=\"" + jester_https_trustStore +
                                                "\" -C TRUST_STORE_PASSWORD=\"" + jester_https_trustStorePassword +
                                                "\" -C SSL_PROTOCOL=\"" + jester_https_sslProtocol +
                                                "\" services/jester/router.ol", .type="Jolie"} )( Jester.location )
    config@Jester( config )()

    // testing jester invoking the client
    scope( call ) {
        install( Fault404 =>  throw( TestFailed, "getOrders - Received 404 when 200 was expected" ) )
        with( rq ) {
            ._puserId = "ciao";
            ._qmaxItems = 10
        }
        getOrders@Test( rq )( rs )
    }

    undef( rq )
    scope( call ) {
        install( Fault404 =>  throw( TestFailed, "putOrder - Received 404 when 200 was expected" ) )
        with( rq.body ) {
            .userId = "ciao"
            with( .order ) {
                .title = "title";
                .id = 5;
                .date = "04.04.2004"
            }
        }
        putOrder@Test( rq )( rs )
    }

    undef( rq )
    scope( call ) {
        install( Fault404 =>  throw( TestFailed, "deleteOrder - Received 404 when 200 was expected" ) )
        with( rq.body ) {
            .orderId = 5
        }
        deleteOrder@Test( rq )( rs )
    }

    undef( rq )
    scope( call ) {
        install( Fault404 =>  throw( TestFailed, "getUser - Received 404 when 200 was expected" ) )
        rq << {
            ._pcountry = "USA"
            body << {
                city = "Springfield"
                surname = "White"
            }
        }
        getUsers@Test( rq )( rs )
    }

    if ( #rs.users != 2 ) {
        throw( TestFailed, "Expected 2 users in getUsers operation")
    }

    undef( rq ) 
    scope( call ) {
        install( Fault404 =>  throw( TestFailed, "getOrderdsByItem (0) - Received 404 when 200 was expected" ) )
        with( rq.body ) {
            .userId = "ciao";
            .itemName= "ciao";
            .quantity = 0
        }
        getOrdersByItem@Test( rq )( rs )
    }
   
    undef( rq )
    scope( call ) {
        install( Fault404 =>  throw( TestFailed, "getOrderdsByItem (1) - Received 404 when 200 was expected" ) )
        with( rq.body ) {
            .userId = "ciao";
            .itemName= "ciao"
        }
        getOrdersByItem@Test( rq )( rs )
    }

    undef( rq )
    scope( call ) {
        install( Fault404 =>  throw( TestFailed, "getOrderdsByItem (2) - Received 404 when 200 was expected" ) )
        with( rq.body ) {
            .userId = "ciao"
        }
        getOrdersByItem@Test( rq )( rs )
    }


    undef( rq )
    scope( fault ) {
        install( FaultTest => nullProcess )
        with( rq.body ) {
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