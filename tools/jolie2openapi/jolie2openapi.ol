from console import Console
from file import File
from openapi import OpenApi
from openapi import RestApiTemplate
from args import Args
from metajolie import MetaJolie

constants {
    RESTTEMPLATE = "rest-template.json"
}

service Jolie2OpenApi {

    embed Console as Console 
    embed File as File
    embed OpenApi as OpenApi
    embed Args as Args
    embed MetaJolie as MetaJolie


    init {
        install( Error => nullProcess )
        install( Help => nullProcess )
        install( ParametersError => nullProcess )
        install( DefinitionError => println@Console( main.DefinitionError )() )
    }

    main {
        // default values
        scheme = "http"
        wkdir = "."
        level0 = false
        template_file = RESTTEMPLATE
        openApiVersion = "3.0"
        documentVersion = "1.0"

        scope( get_args ) {
            install( Help => nullProcess )
            getParsedArgs@Args({
                args << args
                argsMap[ 0 ] << { 
                    name = "service-filename"
                    optional = false
                    description = "Filename of the jolie service to be converted"
                    isFlag = false
                }
                argsMap[ 1 ] << { 
                    name = "input-port"
                    optional = false
                    description = "Input port to be converted"
                    isFlag = false
                }
                argsMap[ 2 ] << { 
                    name = "router-host"
                    optional = false
                    description = "Url of the host to be contacted for using rest apis"
                    isFlag = false
                }
                argsMap[ 3 ] << { 
                    name = "output-folder", 
                    optional = true,
                    description = "Output folder where storing the resulting json file. Default is the current folder",
                    isFlag = false
                }
                argsMap[ 4 ] << { 
                    name = "scheme", 
                    optional = true,
                    description = "http or https. Default is http",
                    isFlag = false
                }
                argsMap[ 5 ] << { 
                    name = "level0", 
                    optional = true,
                    description = "If set no external templates from rest-template.json will be applied",
                    isFlag = true
                }
                argsMap[ 6 ] << { 
                    name = "template-file", 
                    optional = true,
                    description = "Name of the rest api template file. Default is rest-template.json",
                    isFlag = false
                }
                argsMap[ 7 ] << { 
                    name = "openapi-version", 
                    optional = true,
                    description = "Version of the OpenApi to be conformant with (2.0 | 3.0). Default is 3.0",
                    isFlag = false
                }
                argsMap[ 8 ] << { 
                    name = "document-version", 
                    optional = true,
                    description = "Version of the generated document. Default is 1.0",
                    isFlag = false
                }

            })( arguments )

            // conversion od arguments into a hashmap
            for ( a in arguments.parsedArgs ) {
                arg_hashmap.( a.name ) << a
            }

            service_filename = arg_hashmap.("service-filename").value
            input_port = arg_hashmap.("input-port").value
            router_host = arg_hashmap.("router-host").value
            
            if ( is_defined( arg_hashmap.("output-folder") ) ) {
                wkdir = arg_hashmap.("output-folder").value
            }
            if ( is_defined( arg_hashmap.scheme ) ) {
                scheme = arg_hashmap.scheme.value
            }
            if ( is_defined( arg_hashmap.level0 ) ) {
                level0 = true
            }
            if ( is_defined( arg_hashmap.("template-file") ) ) {
                template_file = arg_hashmap.("template-file").value
            }
            if ( is_defined( arg_hashmap.("openapi-version") ) ) {
                openApiVersion = arg_hashmap.("openapi-version").value
            }
            if ( is_defined( arg_hashmap.("document-version") ) ) {
                documentVersion = arg_hashmap.("document-version").value
            }
            
            


            if( !level0 ) {
                scope( load_template ) {
                    install( FileNotFound => 
                            println@Console( template_file + "not found")()
                            throw( Error )
                    )
                    readFile@File( {
                        filename = template_file
                        format = "json"
                    } )( template )
                    if ( !( template instanceof RestApiTemplate ) ) {
                        println@Console("Error: Template file " + template_file + " is not a valid RestApiTemplate.")()
                        println@Console("RestApiTemplate must be a json file, conformant to the following type (expressed in jolie language):\n
    type RestApiTemplate {
        operations* {
            operation: string 
            path?: string
            method: string( enum([\"get\",\"post\",\"put\",\"delete\",\"patch\"]))
            faultsMapping* {
                jolieFault: string
                httpCode: int    
            }
        }
    }")()
                    }
                }
            }

            getInputPortMetaData@MetaJolie( { filename = service_filename } )( meta )
            for ( p in meta.input ) {
                if ( p.name == input_port ) {
                    port << p
                }
            }
            if ( port instanceof void ) {
                println@Console("Error: Input port " + input_port + " not found in service " + service_filename)()
                throw( Error )
            }

            openapi << {
                //filename = service_filename
                host = router_host
                //inputPort = service_input_port
                level0 = level0
                port << port
                scheme  = scheme
                version = documentVersion
                openApiVersion = openApiVersion

                
            }

            if ( !level0 ) {
                openapi.template -> template
            }

            println@Console("Creating openapi file... " )()
            getOpenApiFromJolieMetaData@OpenApi( openapi )( f.content );
            writeFile@File( {
                filename = wkdir + "/" + service_input_port + ".json"
                format = "text"
            } )()
            println@Console("Done.")()
        }
        
    }
}