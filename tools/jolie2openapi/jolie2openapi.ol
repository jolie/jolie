from console import Console
from file import File
from openapi import OpenApi



service Jolie2OpenApi {

    embed Console as Console 
    embed File as File
    embed OpenApi as OpenApi


    constants {
        RESTTEMPLATE = "rest_template.json"
    }

    init {
        install( Error => nullProcess )
        install( DefinitionError => println@Console( main.DefinitionError )() )
    }

    main {
        if ( #args < 4 || #args > 6 ) {
            println@Console("Usage: jolie2openapi <service_filename> <input_port> <router_host> <output_folder> <scheme> [level0 true|false]")()
            println@Console("<service_filename>:\tfilename of the jolie service")()
            println@Console("<input_port>:\tinput port to be converted")()
            println@Console("<router_host>:\turl of the host to be contacted for using rest apis")()
            println@Console("<output_folder>:\toutput folder where storing the resulting json file")()
            println@Console("<scheme>:\t http or https")()
            
            println@Console("[level0 true|false]:\t if set to true no external templates from rest_template.json will be applied. Default is false.")()
            
            println@Console()()
            throw( Error )
        }
        
        
        service_filename = args[ 0 ]
        service_input_port = args[ 1 ]
        router_host = args [ 2 ]
        wkdir = args[ 3 ]
        scheme = args[ 4 ]

        if ( #args == 6 ) {
            level0 = bool( args[ 5 ] )
        } else {
            level0 = false
        }

        if( !level0 ) {
            scope( load_template ) {
                install( FileNotFound => 
                        println@Console("rest_template.json not found")()
                        throw( Error )
                )
                readFile@File( {
                    filename = RESTTEMPLATE
                    format = "json"
                } )( template )
            }
        }

        openapi << {
            //filename = service_filename
            host = router_host
            //inputPort = service_input_port
            level0 = level0


            
        }

        if ( !level0 ) {
            openapi.template -> template
        }

        port: Port
            scheme: string( enum(["http","https"]))
            version: string   // version of the document
            template {
                operations* {
                    operation: string 
                    path?: string
                    method: string( enum(["get","post","put","delete","patch"]))
                    faultsMapping* {
                        jolieFault: string
                        httpCode: int    
                    }
                }
            }
            openApiVersion: string( enum(["2.0","3.0"]))    // version of the openapi

        println@Console("Creating json file... " )()
        getOpenApiFromJolieMetaData@OpenApi( openapi )( f.content );
        f.format = "text";
        f.filename = wkdir + "/" + service_input_port + ".json"
        writeFile@File( f )()
        println@Console("Done.")()
        
    }
}