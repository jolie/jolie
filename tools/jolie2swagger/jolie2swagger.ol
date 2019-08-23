include "services/swagger/public/interfaces/Jolie2SwaggerInterface.iol"
include "console.iol"
include "file.iol"

outputPort Jolie2Swagger {
    Interfaces: Jolie2SwaggerInterface
}

embedded {
    Jolie:
        "services/swagger/jolie2swagger.ol" in Jolie2Swagger
}

constants {
    TEMPLATEFILE = "rest_template.json"
}

init {
    install( Error => nullProcess )
}

main {
    if ( #args < 4 || #args > 5 ) {
        println@Console("Usage: jolie2swagger <service_filename> <input_port> <router_host> <output_folder> [easy_interface true|false]")()
        println@Console("<service_filename>:\tfilename of the jolie service")()
        println@Console("<input_port>:\tinput port to be converted")()
        println@Console("<router_host>:\turl of the host to be contacted for using rest apis")()
        println@Console("<output_folder>:\toutput folder where storing the resulting json file")()
        println@Console("[easy_interface true|false]:\t if true no templates will be exploited for generating the json file. Default is false. jolie2swagger will read templates from file rest_template.json")()
        println@Console()()
        throw( Error )
    }
    
    
    service_filename = args[ 0 ]
    input_port = args[ 1 ]
    router_host = args [ 2 ]
    output_folder = args[ 3 ]

    if ( #args == 5 ) {
        easy_interface = args[ 4 ]
    } else {
        easy_interface = false
    }

    if( !easy_interface ) {
        f.filename = TEMPLATEFILE
        f.format = "json"
        readFile@File( f )( template )
    }

    with( swagger ) {
        .filename = service_filename
        .host = router_host
        .inputPort = service_input_port
        .easyInterface = easy_interface
        if ( !easy_interface ) {
            .template -> template
        }
    }

    println@Console("Creating json file... " )()
    jolie2swagger@Jolie2Swagger( swagger )( f.content )
    f.filename = wkdir + "/" + service_input_port + ".json"
    writeFile@File( f )()
    println@Console("Done.")()
    
}