include "services/jester/JesterConfiguratorInterface.iol"
include "services/jester/router.iol"
include "console.iol"
include "file.iol"
include "runtime.iol"
include "metajolie.iol"
include "metarender.iol"

outputPort JesterConfigurator {
    Interfaces: JesterConfiguratorInterface
}

outputPort Jester {
    Interfaces: RouterIface
}

embedded {
    Jolie:
        "services/jester/jester_configurator.ol" in JesterConfigurator
}

constants {
    TEMPLATEFILE = "rest_template.json"
}

init {
    install( Error => nullProcess )
}

main {
    if ( #args < 3 || #args > 13 ) {
     println@Console("Usage: jolier <service_filename> <input_port> <router_host> [-easyInterface] [-debug] [-keyStore] [-keyStorePassword] [-trustStore] [-trustStorePassword] [-sslProtocol]")()
        println@Console("<service_filename>:\tfilename of the jolie service.")()
        println@Console("<input_port>:\tinput port to be converted. Note that the inputPort location must be set to value \"local://JesterEmbedded\"")()
        println@Console("<router_host>:\turl of the host to be contacted for using rest apis")()
        println@Console("[-easyInterface]:\t if set no templates will be exploited for generating the json file. Default is false. jolier will read templates from file rest_template.json")()
        println@Console("[-debug]:\t when set it enables the debug modality, default is false")()
        println@Console("[-keyStore]:\t  when sets the keyStore location")()
        println@Console("[-keyStorePassword]:\t  sets the keyStore password")()
        println@Console("[-trustStore]:\t sets the trustStore location")()
        println@Console("[-trustStorePassword]:\t sets the trustStore password")()
        println@Console("[-sslProtocol]:\t sets the ssl Protocol")()
        println@Console()()
        throw( Error )
    }
    
    /* preparing data */
    
    service_filename = args[ 0 ]
    service_input_port = args[ 1 ]
    router_host = args [ 2 ]
    easy_interface = false
    debug = false

    jester_http_location = "socket://" + router_host

    if ( #args > 3 ) {
        i = 3 
        while (i < #args ) {
            if ( args[ i ] == "-easyInterface" ) {
                easy_interface = true
                i++
            } else if ( args[ i ] == "-debug" ) {
                debug = true
                i++
            } else if ( args[ i ] == "-keyStore" ) {
                jester_https_keyStore = args[ i + 1 ]
                i = i + 2
            } else if ( args[ i ] == "-keyStorePassword" ) {
                jester_https_keyStorePassword = args[ i + 1 ]
                i = i + 2
            } else if ( args[ i ] == "-trustStore" ) {
                jester_https_trustStore = args[ i + 1 ]
                i = i + 2
            }else if ( args[ i ] == "-trustStorePassword" ) {
                jester_https_trustStorePassword = args[ i + 1 ]
                i = i + 2
            }else if ( args[ i ] == "-sslProtocol" ) {
                jester_https_sslProtocol = args[ i + 1 ]
                i = i + 2
            } else {
                println@Console("Argument " + args[ i ] + " not recognized")()
                throw( Error )
            }
        }
    }
    
    if ( debug ) {
        debug_string = "--trace -C DEBUG=true"
    } else {
        debug_string = "-C DEBUG=false"
    }

    /* execution */
    if( !easy_interface ) {
        scope( read_template ) {
            install( default => 
                println@Console("Template file " + TEMPLATEFILE + " not found " )()
                throw( Error )
            )
            f.filename = TEMPLATEFILE
            f.format = "json"
            readFile@File( f )( template )
        }
    }

    with( jester ) {
        .filename = service_filename;
        .host = router_host;
        .inputPort = service_input_port;
        .easyInterface = easy_interface;
        if ( !easy_interface ) {
            .template -> template
        }
    }

    println@Console("Getting outputPort definition...")();
    with( request_meta ) {
        .filename = service_filename
    }
    getInputPortMetaData@MetaJolie( request_meta )( metadata )
    for( i = 0, i < #metadata.input, i++ ) {
        // port selection from metadata
        if ( metadata.input[ i ].name == service_input_port ) {
              getSurface@MetaRender( metadata.input[i] )( surface )
        }
    }
    undef( f )
    f.content = surface + "\nembedded { Jolie: \"" + service_filename + "\" in " + service_input_port + " }\n"
    f.filename = "jester_config.iol"
    writeFile@File( f )()
    
    println@Console("Creating jester config from templates... " )()
    getJesterConfig@JesterConfigurator( jester )( config );

    println@Console("Running jester...")()
    if (is_defined(jester_https_keyStore)){
        jester_https_location = jester_http_location
        jester_http_location = "local"
    }else{
       jester_https_location = "local"
    }
    loadEmbeddedService@Runtime( { .filepath = debug_string + " -C API_ROUTER_HTTP=\"" + jester_http_location + 
                                                            "\" -C API_ROUTER_HTTPS=\"" + jester_https_location +
                                                            "\" -C KEY_STORE=\"" + jester_https_keyStore +
                                                            "\" -C KEY_STORE_PASSWORD=\"" + jester_https_keyStorePassword +
                                                            "\" -C TRUST_STORE=\"" + jester_https_trustStore +
                                                            "\" -C TRUST_STORE_PASSWORD=\"" + jester_https_trustStorePassword +
                                                            "\" -C SSL_PROTOCOL=\"" + jester_https_sslProtocol +
                                                            "\" services/jester/router.ol", .type="Jolie"} )( Jester.location )
    config@Jester( config )()
    linkIn( lock )
    
}