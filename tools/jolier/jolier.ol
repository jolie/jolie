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
    if ( #args < 3 || #args > 5 ) {
        println@Console("Usage: jolier <service_filename> <input_port> <router_host> [-easyInterface] [-debug]")()
        println@Console("<service_filename>:\tfilename of the jolie service.")()
        println@Console("<input_port>:\tinput port to be converted. Note that the inputPort location must be set to value \"local://JesterEmbedded\"")()
        println@Console("<router_host>:\turl of the host to be contacted for using rest apis")()
        println@Console("[-easyInterface]:\t if set no templates will be exploited for generating the json file. Default is false. jolier will read templates from file rest_template.json")()
        println@Console("[-debug]:\t when set it enables the debug modality, default is false")()
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
        for( i = 3, i < #args, i++ ) {
            if ( args[ i ] == "-easyInterface" ) {
                easy_interface = true
            } else if ( args[ i ] == "-debug" ) {
                debug = true
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
        f.filename = TEMPLATEFILE
        f.format = "json"
        readFile@File( f )( template )
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
              getSurface@MetaRender( metadata.input )( surface )
        }
    }
    undef( f )
    f.content = surface + "\nembedded { Jolie: \"" + service_filename + "\" in " + service_input_port + " }\n"
    f.filename = "jester_config.iol"
    writeFile@File( f )()
    
    println@Console("Creating jester config from templates... " )()
    getJesterConfig@JesterConfigurator( jester )( config );

    println@Console("Running jester...")()
    loadEmbeddedService@Runtime( { .filepath = debug_string + " -C API_ROUTER_HTTP=\"" + jester_http_location + "\" services/jester/router.ol", .type="Jolie"} )( Jester.location )
    config@Jester( config )()
    linkIn( lock )
    
}