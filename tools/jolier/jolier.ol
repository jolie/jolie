include "services/jester/JesterConfiguratorInterface.iol"
include "console.iol"
include "file.iol"
include "runtime.iol"

outputPort JesterConfigurator {
    Interfaces: JesterConfiguratorInterface
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
    if ( #args < 4 || #args > 6 ) {
        println@Console("Usage: jolier <service_filename> <input_port> <router_host> <output_folder> [-easyInterface] [-debug]")()
        println@Console("<service_filename>:\tfilename of the jolie service")()
        println@Console("<input_port>:\tinput port to be converted")()
        println@Console("<router_host>:\turl of the host to be contacted for using rest apis")()
        println@Console("<output_folder>:\toutput folder where storing the resulting json file")()
        println@Console("[-easyInterface]:\t if set no templates will be exploited for generating the json file. Default is false. jolier will read templates from file rest_template.json")()
        println@Console("[-debug]:\t when set it enables the debug modality, default is false")()
        println@Console()()
        throw( Error )
    }
    
    /* preparing data */
    
    service_filename = args[ 0 ]
    service_input_port = args[ 1 ]
    router_host = args [ 2 ]
    wkdir = args[ 3 ]
    easy_interface = false
    debug = false

    jester_http_location = "socket://" + router_host

    if ( #args > 4 ) {
        for( i = 4, i < #args, i++ ) {
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

    println@Console("Creating jester config file... " )()
    getJesterConfig@JesterConfigurator( jester )( f.content );
    f.format = "text"
    f.filename = wkdir + "/jester_config.iol"
    writeFile@File( f )()
    println@Console("Done.")()

    println@Console("Running jester...")()
    loadEmbeddedService@Runtime( { .filepath = debug_string + " -C API_ROUTER_HTTP=\"" + jester_http_location + "\" services/jester/router.ol", .type="Jolie"} )()
    linkIn( lock )
    
}