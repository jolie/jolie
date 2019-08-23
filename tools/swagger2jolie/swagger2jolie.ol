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

include "console.iol"
include "string_utils.iol"
include "file.iol"
include "json_utils.iol"

include "services/swagger/public/interfaces/SwaggerDefinitionInterface.iol"


outputPort SwaggerDefinition {
    Interfaces: SwaggerDefinitionInterface
}

outputPort HTTP {
  Protocol: http {
      .method = "get";
      .osc.getDefinition.alias -> alias
  }
  RequestResponse: getDefinition
}

outputPort HTTPS {
  Protocol: https {
      .ssl.protocol = "TLSv1.1";
      .method = "get";
      .osc.getDefinition.alias -> alias
  }
  RequestResponse: getDefinition
}

embedded {
  Jolie:
    "services/swagger/swagger_definition.ol" in SwaggerDefinition
}

define _get_protocol_port {
    if ( protocol == "http" ) {
        protocol_port = "80"
    } else if ( protocol == "https" ) {
        protocol_port = "443"
    } else {
        println@Console("Protocol not supported:" + protocol.result[0] )();
        throw( Error )
    }
}

init {
    registerForInput@Console()();

    install( Error => nullProcess )
}

main {
    if ( #args < 3 || #args > 4 ) {
        println@Console("Usage: swagger2jolie <url|filepath> <service_name> <output_folder> [protocol http|https]")();
        throw( Error )
    }
    ;
    println@Console("Generating client...")();
    if ( #args == 4 && (args[ 3 ] == "http" || args[ 3 ] == "https")) {
        protocol = args[ 3 ]
    } else {
        protocol = ""
    };
    output_folder = args[ 2 ];

    exists@File( output_folder )( exists_folder );
    if ( !exists_folder ) {
          println@Console( "Output folder " + output_folder + " does not exist. Error!")();
          throw( Error )
    };

    // try to get the json declaration
    scope( get_swagger_file ) {
        install( FileNotFound => 
            
            url = spl = args[0];

            if ( protocol == "" ) {
                    spl.regex = ":";
                    split@StringUtils( spl )( protocol_split );
                    protocol = protocol_split.result[0]
            }
            ;
            _get_protocol_port;


            url.replacement = "socket";
            url.regex = protocol;
            replaceAll@StringUtils( url )( location );
            if ( #protocol_split.result == 2 ) {
                /* add default port number */
                spl.regex = "/";
                split@StringUtils( spl )( splitted_url );
                location = ""; alias = "";
                for( sp = 0, sp < #splitted_url.result, sp++ ) {
                    if ( sp < 3 ) {
                            location = location + splitted_url.result[sp];
                        if ( sp == 2 ) {
                            location = location + ":" + protocol_port
                        } else {
                            location = location + "/"
                        }
                    } else {
                        alias = alias + splitted_url.result[sp];
                        if ( sp < (#splitted_url.result - 1) ) {
                            alias = alias + "/"
                        }
                    }
                }
            };
            location.replacement = "socket";
            location.regex = protocol;
            replaceAll@StringUtils( location )( location );
            if ( protocol == "http" ) {
                HTTP.location = location;
                getDefinition@HTTP()( swagger )
            } else if ( protocol == "https" ) {
                HTTPS.location = location;
                getDefinition@HTTPS()( swagger )
            }
        );
        f.filename = args[ 0 ];
        f.format = "json";
        readFile@File( f )( swagger );
        _get_protocol_port
    }
    ;

    service_name = args[ 1 ];
    spl = string( swagger.host );
    spl.regex = ":";
    split@StringUtils( spl )( ports );
    if ( #ports.result == 1 ) {
        swagger.host = swagger.host + ":" + protocol_port
    };

    /* creating outputPort */
    outputPort.name = service_name + "Port";
    outputPort.location = "socket://" + swagger.host + swagger.basePath;
    outputPort.protocol = protocol;
    outputPort.interface = service_name + "Interface";

    foreach( path : swagger.paths ) {
        foreach( method : swagger.paths.( path ) ) {
            if ( !is_defined( swagger.paths.( path ).( method ).operationId ) ) {
                println@Console( "Path " + method + ":" + path + " does not define an operationId, please insert an operationId for this path.")();
                print@Console(">")();
                in( operationId )
            } else {
                operationId = swagger.paths.( path ).( method ).operationId
            }
            ;
            op_max = #outputPort.interface.operation;
            spl = path;
            spl.replacement = "%!\\{";
            spl.regex = "\\{";
            replaceAll@StringUtils( spl )( corrected_path );

            outputPort.interface.operation[ op_max ] = operationId;
            outputPort.interface.operation[ op_max ].path = corrected_path;
            outputPort.interface.operation[ op_max ].method = method;
            outputPort.interface.operation[ op_max ].parameters << swagger.paths.( path ).( method ).parameters;
            foreach( res : swagger.paths.( path ).( method ).responses ) {
                if ( res == "200" ) {
                     outputPort.interface.operation[ op_max ].response << swagger.paths.( path ).( method ).responses.( res )
                } else {
                     outputPort.interface.operation[ op_max ].faults.( res ) << swagger.paths.( path ).( method ).responses.( res )
                }
            }
            
        }
    };

    foreach( definition : swagger.definitions ) {
        get_def.name = definition;
        get_def.definition -> swagger.definitions.( definition );
        getJolieTypeFromSwaggerDefinition@SwaggerDefinition( get_def )( j_definition );
        interface_file = interface_file + j_definition
    };

    for( o = 0, o < #outputPort.interface.operation, o++ ) {
        rq_p.definition.parameters -> outputPort.interface.operation[ o ].parameters;
        rq_p.name = outputPort.interface.operation[ o ] + "Request";
        getJolieTypeFromSwaggerParameters@SwaggerDefinition( rq_p )( parameters );

        type_string = parameters;

        type_string = type_string + "type " + outputPort.interface.operation[ o ] + "Response:";
        if ( is_defined( outputPort.interface.operation[ o ].response.schema ) ) {
            if ( is_defined( outputPort.interface.operation[ o ].response.schema.("$ref") ) ) {
                getReferenceName@SwaggerDefinition( outputPort.interface.operation[ o ].response.schema.("$ref") )( ref );
                type_string = type_string + ref + "\n"
            } else if ( outputPort.interface.operation[ o ].response.schema.type == "array" ) {
                rq_arr.definition -> outputPort.interface.operation[ o ].response.schema;
                rq_arr.indentation = 1;
                getJolieDefinitionFromSwaggerArray@SwaggerDefinition( rq_arr )( array );
                type_string = type_string + " void {\n\t._" + array + "}\n"
            } else if ( outputPort.interface.operation[ o ].response.schema.type == "object" ) {
                type_string = type_string + "undefined\n"
            } else {
                rq_n.type = outputPort.interface.operation[ o ].response.schema.type;
                if ( is_defined( outputPort.interface.operation[ o ].response.schema.format ) ) {
                    rq_n.format = outputPort.interface.operation[ o ].response.schema.format
                };
                getJolieNativeTypeFromSwaggerNativeType@SwaggerDefinition( rq_n )( native );
                type_string = type_string + native + "\n"
            }
        } else {
          type_string = type_string + "undefined \n"
        }
        ;
        interface_file = interface_file + type_string
    };


    /* create interface file */
    interface_file = interface_file + "interface " + outputPort.interface + "{\n";
    interface_file = interface_file + "RequestResponse:\n";
    for( o = 0, o < #outputPort.interface.operation, o++ ) {
        interface_file = interface_file + "\t" + outputPort.interface.operation[ o ]
            + "( " + outputPort.interface.operation[ o ] + "Request )"
            + "( " + outputPort.interface.operation[ o ] + "Response )";
        if ( is_defined( outputPort.interface.operation[ o ].faults ) ) {
            interface_file = interface_file + " throws";
            foreach( f : outputPort.interface.operation[ o ].faults ) {
                faultType = "";
                if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).schema ) ) {
                    getReferenceName@SwaggerDefinition( outputPort.interface.operation[ o ].faults.( f ).schema.("$ref") )( ref );
                    faultType = "(" + ref + ")"
                } else {
                    if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).description ) ) {
                        faultType = "( string )"
                    }
                }
                interface_file = interface_file + " Fault" + f + faultType
            }
        };

        if ( o < (#outputPort.interface.operation - 1) ) {
            interface_file = interface_file + ",\n"
        }
    };
    interface_file =  interface_file + "\n}\n\n";
    file.filename = output_folder + "/" + service_name + "Interface.iol";
    file.content = interface_file;
    writeFile@File( file )()
    ;
    
    //writing client service file
    client_file = "include \"" + service_name + "Interface.iol\"\n\n";
    client_file = client_file + "execution{ concurrent }\n\n";


    client_file = client_file + "outputPort " + outputPort.name + "{\n";
    client_file = client_file + "Location: \"" + outputPort.location + "\"\n";
    client_file = client_file + "Protocol: " + outputPort.protocol + "{\n";
    if ( protocol == "https" ) { 
        client_file = client_file + ".ssl.protocol = \"TLSv1.1\";"
    };
    for( o = 0, o < #outputPort.interface.operation, o++ ) {
        client_file = client_file + ".osc." + outputPort.interface.operation[ o ]
                          + ".alias=\"" + outputPort.interface.operation[ o ].path + "\";\n";
        client_file = client_file + ".osc." + outputPort.interface.operation[ o ]
                          + ".method=\"" + outputPort.interface.operation[ o ].method + "\"";
        if ( o < (#outputPort.interface.operation - 1) ) {
            client_file = client_file + ";\n"
        }
    };
    client_file = client_file + "}\n";

    client_file = client_file + "Interfaces: " + outputPort.interface + "\n}\n\n";

    client_file = client_file + "inputPort " + service_name + "{\n";
    client_file = client_file + "Location:\"local\"\n";
    client_file = client_file + "Protocol: sodep\n";
    client_file = client_file + "Interfaces: " + outputPort.interface + "\n}\n\n";

    client_file = client_file + "main {\n";
    for( o = 0, o < #outputPort.interface.operation, o++ ) {
        client_file = client_file + "\t[ " + outputPort.interface.operation[ o ] + "( request )( response ) {\n";
        client_file = client_file + "\t\t" + outputPort.interface.operation[ o ] + "@" + outputPort.name + "( request )( response )\n";
        if ( is_defined( outputPort.interface.operation[ o ].faults ) ) {
            foreach( f : outputPort.interface.operation[ o ].faults ) {
                faultValue = "";
                if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).schema ) ) {
                    faultValue = ", response"
                } else {
                    if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).description ) ) {
                        faultValue = ", response.description"
                    }
                }
                client_file = client_file + "\t\tif ( response.(\"@header\").statusCode == " + f + ") {\n ";
                client_file = client_file + "\t\t\tthrow( Fault" + f + faultValue + ")\n"
                client_file = client_file + "\t\t}\n"
            }
        };
        client_file = client_file + "\t}]\n\n"
    };
    client_file = client_file + "}"

    undef( file );
    file.filename = output_folder + "/" + service_name + "Client.ol";
    file.content = client_file;
    writeFile@File( file )();

    println@Console("Done.")()
}
