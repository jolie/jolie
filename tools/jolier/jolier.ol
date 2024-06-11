/*
The MIT License (MIT)
Copyright (c) 2016 Claudio Guidi <guidiclaudio@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

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
    if ( #args < 1 || #args > 16 ) {
        println@Console("Usage: jolier createHandler")()
        println@Console("Create an empty Header Handler")()
        println@Console("Usage: jolier <service_filename> <input_port> <router_host> [-easyInterface] [-debug] [-headerHandler] [-keyStore] [filePath] [-keyStorePassword] [password] [-trustStore] [filePath] [-trustStorePassword] [password] [-sslProtocol] [protocol]")()
        println@Console("<service_filename>:\tfilename of the jolie service.")()
        println@Console("<input_port>:\tinput port to be converted. Note that the inputPort location must be set to value \"local://JesterEmbedded\"")()
        println@Console("<router_host>:\turl of the host to be contacted for using rest apis")()
        println@Console("[-easyInterface]:\t if set no templates will be exploited for generating the json file. Default is false. jolier will read templates from file rest_template.json")()
        println@Console("[-debug]:\t when set it enables the debug modality, default is false")()
        println@Console("[-debug-file]:\t when set it enables the file tracing modality, default is false")()
        println@Console("[-headerHandler]:\t when set it enables the header handler service, default is false")()
        println@Console("[-keyStore]:\t  sets the keyStore location")()
        println@Console("[-keyStorePassword]:\t  sets the keyStore password")()
        println@Console("[-trustStore]:\t sets the trustStore location")()
        println@Console("[-trustStorePassword]:\t sets the trustStore password")()
        println@Console("[-sslProtocol]:\t sets the ssl protocol")()
        println@Console()()
        throw( Error )
    }
    
    /* preparing data */
    

    if (#args == 1) {
        if ( args[ 0 ] == "createHandler" ) {
f.content ="type incomingHeaderHandlerRequest:void {
    .operation:string 
    .headers:undefined
}
type incomingHeaderHandlerResponse: undefined

type outgoingHeaderHandlerRequest:void {
    .operation:string 
    .response?:undefined
}
type outgoingHeaderHandlerResponse: undefined

interface HeaderHandlerInterface {
RequestResponse:
    incomingHeaderHandler( incomingHeaderHandlerRequest )( incomingHeaderHandlerResponse ),
    outgoingHeaderHandler( outgoingHeaderHandlerRequest )( outgoingHeaderHandlerResponse )
}

inputPort HeaderPort {
    Location: \"local\"
    Interfaces: HeaderHandlerInterface
}

execution { concurrent }

main {
    [ incomingHeaderHandler( request )( response ) {
        nullProcess
    } ]

    [ outgoingHeaderHandler( request )( response ) {
        nullProcess
    } ]
}"

            f.filename = "RestHandler.ol"
            writeFile@File( f )()
        } else {
            println@Console("Argument " + args[ i ] + " not recognized")()
            throw( Error )
        }
    } else {

    service_filename = args[ 0 ]
    service_input_port = args[ 1 ]
    router_host = args [ 2 ]
    easy_interface = false
    debug = false
    headerHandler = false
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
            } else if ( args[ i ] == "-debug-file" ) {
                debug_file = true
                i++
                i++
            }else if ( args[ i ] == "-headerHandler" ) {
                headerHandler = true
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
    } else if ( debug_file ) {
        debug_string = "--trace file -C DEBUG=false"
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

    if (headerHandler == true){
        println@Console("Getting HeaderHandler...")();
        with( request_meta ) {
            .filename = "RestHandler.ol"
        }
        getInputPortMetaData@MetaJolie( request_meta )( metadata )
        for( i = 0, i < #metadata.input, i++ ) {
            // port selection from metadata
            if ( metadata.input[ i ].name == "HeaderPort" ) {
                getSurface@MetaRender( metadata.input[i] )( surfaceHeaderHandler)
            }
        }
        f.content = surface + "\n"  + surfaceHeaderHandler + "\nembedded { Jolie: \"" + service_filename + "\" in " + service_input_port + " }\n"
                  + "\nembedded { Jolie: \"RestHandler.ol\" in HeaderPort }\n"
        handler_string =" -C HANDLER=true"
    } else{
        f.content = surface + "\nembedded { Jolie: \"" + service_filename + "\" in " + service_input_port + " }\n"
        handler_string =" -C HANDLER=false"
    }
    
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

    loadEmbeddedService@Runtime( { .filepath = debug_string + handler_string + " -C API_ROUTER_HTTP=\"" + jester_http_location + 
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
}