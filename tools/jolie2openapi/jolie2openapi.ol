include "services/openapi/public/interfaces/Jolie2OpenApiInterface.iol"
include "console.iol"
include "file.iol"

outputPort Jolie2OpenApi {
    Interfaces: Jolie2OpenApiInterface
}

embedded {
    Jolie:
        "services/openapi/jolie2openapi.ol" in Jolie2OpenApi
}

constants {
    TEMPLATEFILE = "rest_template.json"
}

init {
    install( Error => nullProcess )
    install( DefinitionError => println@Console( main.DefinitionError )() )
}

main {
    if ( #args < 4 || #args > 6 ) {
        println@Console("Usage: jolie2openapi <service_filename> <input_port> <router_host> <output_folder> [easy_interface true|false]")()
        println@Console("<service_filename>:\tfilename of the jolie service")()
        println@Console("<input_port>:\tinput port to be converted")()
        println@Console("<router_host>:\turl of the host to be contacted for using rest apis")()
        println@Console("<output_folder>:\toutput folder where storing the resulting json file")()
        println@Console("[easy_interface true|false]:\t if set to true no external templates from rest_template.json will be applied. Default is false.")()
        println@Console()()
        throw( Error )
    }
    
    
    service_filename = args[ 0 ]
    service_input_port = args[ 1 ]
    router_host = args [ 2 ]
    wkdir = args[ 3 ]

    if ( #args == 6 ) {
        easy_interface = bool( args[ 5 ] )
    } else {
        easy_interface = false
    }

    if( !easy_interface ) {
        scope( load_template ) {
            install( FileNotFound => 
                                        println@Console("rest_template.json not found")()
                                        throw( Error )
            )

            f.filename = TEMPLATEFILE
            f.format = "json"
            readFile@File( f )( template )
        }
    }

    with( openapi ) {
        .filename = service_filename;
        .host = router_host;
        .inputPort = service_input_port;
        .easyInterface = easy_interface;
        if ( !easy_interface ) {
            .template -> template
        }
    }

    println@Console("Creating json file... " )()
    getOpenApi@Jolie2OpenApi( openapi )( f.content );
    f.format = "text";
    f.filename = wkdir + "/" + service_input_port + ".json"
    writeFile@File( f )()
    println@Console("Done.")()
    
}