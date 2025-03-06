/*

 * Copyright (c) 2016 Claudio Guidi <guidiclaudio@gmail.com>

 *   This program is free software; you can redistribute it and/or modify  
 *   it under the terms of the GNU Library General Public License as       
 *   published by the Free Software Foundation; either version 2 of the    
 *   License, or (at your option) any later version.                       
 *                                                                         
 *   This program is distributed in the hope that it will be useful,       
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 *   GNU General Public License for more details.                          
 *                                                                         
 *   You should have received a copy of the GNU Library General Public     
 *   License along with this program; if not, write to the                 
 *   Free Software Foundation, Inc.,                                       
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
 *                                                                         
 *   For details about the authors of this software, see the AUTHORS file. 
*/

include "console.iol"
include "string_utils.iol"
include "file.iol"
include "json_utils.iol"

include "services/openapi/public/interfaces/OpenApiDefinitionInterface.iol"
include "services/openapi/public/interfaces/OpenApi2JolieInterface.iol"


outputPort OpenApiDefinition {
    Interfaces: OpenApiDefinitionInterface
}

outputPort OpenApi2Jolie {
    Interfaces: OpenApi2JolieInterface
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
      .method = "get";
      .osc.getDefinition.alias -> alias
  }
  RequestResponse: getDefinition
}

embedded {
  Jolie:
    "services/openapi/openapi_definition.ol" in OpenApiDefinition,
    "services/openapi/openapi2jolie.ol" in OpenApi2Jolie
}

define _get_protocol_port {
    if ( protocol == "http" ) {
        protocol_port = "80"
    } else if ( protocol == "https" ) {
        protocol_port = "443"
    } else {
        println@Console( "Protocol not supported/File not found: " + protocol )();
        throw( Error )
    }
}

init {
    registerForInput@Console()();

    install( Error => nullProcess )
}

main {
    if ( #args < 3 || #args > 5 ) {
        println@Console("Usage: openapi2jolie <url|filepath> <service_name> <output_folder> [ <protocol http|https> [ <ssl.protocol> ] ]")();
        throw( Error )
    }
    ;
    println@Console("Generating client...")();
    if ( #args > 3 ) {
        protocol = args[ 3 ]
        if ( args[ 3 ] == "https" && #args == 5 ) {
            ssl_protocol = args[ 4 ]
        } else {
            ssl_protocol = ""
        }
    } else {
        ssl_protocol = ""
        protocol = ""
    };
    output_folder = args[ 2 ];

    exists@File( output_folder )( exists_folder );
    if ( !exists_folder ) {
          println@Console( "Output folder " + output_folder + " does not exist. Error!")();
          throw( Error )
    };

    // try to get the json declaration
    scope( get_openapi_file ) {
        install( FileNotFound => 
            
            url = spl = args[0];

            spl.regex = ":";
            split@StringUtils( spl )( protocol_split );
            protocol = protocol_split.result[0]
            _get_protocol_port;


            url.replacement = "socket";
            url.regex = protocol;
            replaceAll@StringUtils( url )( location );
            if ( #protocol_split.result == 2 ) {
                /* add default port number */
                spl = location
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
            
            if ( protocol == "http" ) {
                HTTP.location = location;
                getDefinition@HTTP()( openapi )
            } else if ( protocol == "https" ) {
                HTTPS.location = location;
                if ( ssl_protocol != "" ) {
                    HTTPS.protocol.ssl.protocol = ssl_protocol
                }
                getDefinition@HTTPS()( openapi )
            }
        );
        f.filename = args[ 0 ];
        f.format = "json";
        readFile@File( f )( openapi );
        _get_protocol_port
    }


    service_name = args[ 1 ];
    spl = string( openapi.host );
    spl.regex = ":";
    split@StringUtils( spl )( ports );
    if ( #ports.result == 1 ) {
        openapi.host = openapi.host + ":" + protocol_port
    };


    with( get_code_from_openapi ) {
        .port_name = service_name;
        .openapi -> openapi
    }
    getJolieInterface@OpenApi2Jolie( get_code_from_openapi )( interface_file )
    file.filename = output_folder + "/" + service_name + "Interface.iol";
    file.content = interface_file;
    writeFile@File( file )()
    
    get_code_from_openapi.protocol = protocol
    getJolieClient@OpenApi2Jolie( get_code_from_openapi )( client_file )

    undef( file );
    file.filename = output_folder + "/" + service_name + "Client.ol";
    file.content = client_file;
    writeFile@File( file )();

    println@Console("Done.")()
}
