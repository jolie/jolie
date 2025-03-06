
/*

 * Copyright (c) 2016 Fabrizio Montesi <famontesi@gmail.com>
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

include "uri_templates.iol"
include "reflection.iol"
include "console.iol"
include "string_utils.iol"

include "./router.iol"

execution { concurrent }

include "jester_config.iol"

interface WebIface {
RequestResponse:
	get, post, put, delete, options
}

/* parameters 
API_ROUTER_HTTP: it is the http location of the router
DEBUG: enable DEBUG
*/

outputPort JesterEmbedded {
	Location: "local://JesterEmbedded"
}

inputPort WebInput {
Location: API_ROUTER_HTTP
Protocol: http {
	.debug=DEBUG;
	.debug.showContent=DEBUG;
	.default.get = "get";
	.default.post = "post";
	.default.put = "put";
	.default.delete = "delete";
	.default.options = "options";
	.method -> method;
	.headers.authorization = "authorization";
	.response.headers.("Access-Control-Allow-Methods") = "POST,GET,DELETE,PUT,OPTIONS";
	.response.headers.("Access-Control-Allow-Origin") = "*";
	.response.headers.("Access-Control-Allow-Headers") = "Content-Type";
	.response.headers -> responseOutgoingHeaders;
	.statusCode -> statusCode;
	.format = "json"
}
Interfaces: WebIface
}



inputPort WebInputHttps {
Location: API_ROUTER_HTTPS
Protocol: https {
	.debug=DEBUG;
	.debug.showContent=DEBUG;
	.default.get = "get";
	.default.post = "post";
	.default.put = "put";
	.default.delete = "delete";
	.default.options = "options";
	.method -> method;
	.headers.authorization = "authorization";
	//.response.headers.("Access-Control-Allow-Methods") = "POST,GET,DELETE,PUT,OPTIONS";
	//.response.headers.("Access-Control-Allow-Origin") = "*";
	//.response.headers.("Access-Control-Allow-Headers") = "Content-Type";
	.response.headers -> responseOutgoingHeaders;
	.statusCode -> statusCode;
    .ssl.keyStore= KEY_STORE;
    .ssl.keyStorePassword= KEY_STORE_PASSWORD;
    .ssl.trustStore = TRUST_STORE;
    .ssl.trustStorePassword= TRUST_STORE_PASSWORD;
	.ssl.protocol=SSL_PROTOCOL;
	.format = "json"
}
Interfaces: WebIface
}

inputPort RouterIn {
Location: "local"
Interfaces: RouterIface
}

define split_urls {
  // __input_str
	undef( __str_elements );
  split_str = __input_str;
  split_str.regex = "\\?";
  split@StringUtils( split_str )( splitted_path_string )	
  split_str = splitted_path_string.result[0];
  split_str.regex = "/";
  split@StringUtils( split_str )( splitted_str );
  __str_elements.element_url << splitted_str.result;
  if ( #splitted_path_string.result > 1 ) {
      split_str = splitted_path_string.result[ 1 ];
      split_str.regex = "&";
      split@StringUtils( split_str )( splitted_str );
      for( _s = 0, _s < #splitted_str.result, _s++ ) {
          split_str2 = splitted_str.result[ _s ];
          split_str2.regex = "=";
          split@StringUtils( split_str2 )( splitted_str2 );
          __str_elements.element_query.(splitted_str2.result[ 0 ]).value = splitted_str2.result[ 1 ] 
      }
  }
  
}

define matchTemplate {
	/* __input_str */

  undef( template_elements );
	split_urls;
	template_elements << __str_elements;
	found = false;

	if ( #template_elements.element_url == #uri_elements.element_url ) {
		found = true;
		_e = 0;
		while( found && _e < #uri_elements.element_url ) {
				if ( uri_elements.element_url[ _e ] != template_elements.element_url[ _e ] ) {
						w = template_elements.element_url[ _e ];
						w.regex = "\\{(.*)\\}";
						find@StringUtils( w )( is_param );
						if ( is_param == 0 ) {
								found = false
						} else {
								found.( is_param.group[1] ) = uri_elements.element_url[ _e ]
						}
				}
				;
				_e++
		}
};

if (found){
	foreach (query_element : uri_elements.element_query){
		if (!is_defined(template_elements.element_query.(query_element))){
			found = false
		}else{
			w = template_elements.element_query.(query_element).value;
			w.regex = "\\{(.*)\\}";
			find@StringUtils( w )( is_param );
			if ( is_param == 0 ) {
					found = false
			} else {
					found.( is_param.group[1] ) = uri_elements.element_query.(query_element).value
			}
		}
	} 
}
}

define findRoute
{
	__input_str = request.requestUri;
	split_urls;
	uri_elements << __str_elements;
	for( i = 0, i < #routes && !found, i++ ) {
		if ( routes[i].method == method ) {
			__input_str = routes[i].template;
			matchTemplate;
			op = routes[i].operation;
			outputPort = routes[i].outputPort;
			undef( cast );
			cast << routes[i].cast
		}
	}
}
define headerHandler{
   findRoute;
   invokeRequestHearder.data.operation = method
   invokeRequestHearder.data.headers << request
   invokeRequestHearder.operation = "incomingHeaderHandler"
   invokeRequestHearder.outputPort = "HeaderPort"
   invoke@Reflection( invokeRequestHearder )( responseHandler )
   undef (request.authorization)
   foreach( n : responseHandler ) {
			invokeReq.data.(n) << responseHandler.(n)
	}
}

define headerHandlerResponse{

   undef (invokeRequestHearder)
   invokeRequestHearder.data.operation = method
   invokeRequestHearder.data.response << response
   invokeRequestHearder.operation = "outgoingHeaderHandler"
   invokeRequestHearder.outputPort = "HeaderPort"
   invoke@Reflection( invokeRequestHearder )( invokeReponseHeader ) 
   foreach( n : invokeReponseHeader ) {
	       if (is_defined (invokeReponseHeader.(n).deleteResponseNode) ){
			   undef (response.(invokeReponseHeader.(n).deleteResponseNode))
		   }
		   responseOutgoingHeaders.(n) = invokeReponseHeader.(n)
			
	}
}

define route
{
	findRoute;

	if ( !found ) {
		statusCode = 404
	} else {
		statusCode = 200;
		with( invokeReq ) {
			.operation = op;
			.outputPort = outputPort
		};
	foreach( n : request.data ) {
			invokeReq.data.(n) << request.data.(n)
	};	

	foreach( n : found ) {
			if ( is_defined( cast.( n ) ) ) {
					if ( cast.( n ) == "int" ) {
					if (#invokeReq.data.(n)>1){	
                      for(counter=0, counter<#invokeReq.data.(n) ,counter++){
						  invokeReq.data.(n)[counter] = int( invokeReq.data.(n)[counter] )
					  }
					}else{
						invokeReq.data.(n) = int (found.(n))
					}

					} else if ( cast.( n ) == "long" ) {
					 if (#invokeReq.data.(n)>1){		
                      for(counter=0, counter<#invokeReq.data.(n) ,counter++){
						  invokeReq.data.(n)[counter] = long( invokeReq.data.(n)[counter] )
					  }
					 }else{
						 invokeReq.data.(n) = long (found.(n))
					 }
					} else if ( cast.( n ) == "double" ) {
					 if (#invokeReq.data.(n)>1){	
                      for(counter=0, counter<#invokeReq.data.(n) ,counter++){
						  invokeReq.data.(n)[counter] = double( invokeReq.data.(n)[counter] )
					  }
					 }else{
						  invokeReq.data.(n) = double (found.(n))
					 }
					} else if ( cast.( n ) == "bool" ) {
					 if (#invokeReq.data.(n)>1){	
                      for(counter=0, counter<#invokeReq.data.(n) ,counter++){
						  invokeReq.data.(n)[counter] = bool( invokeReq.data.(n)[counter] )
					  }
					 }else{
						 invokeReq.data.(n) = bool (found.(n))
					 }
						
					}
			} else {
				  /* all the other cases */
	           if (#invokeReq.data.(n)>1){
				for(counter=0, counter<#invokeReq.data.(n) ,counter++){
						invokeReq.data.(n)[counter] << invokeReq.data.(n)[counter]
					}	
			   }else{
				   invokeReq.data.(n) << found.(n)
			   }			
		  }
		}

		scope( invoke_scope ) {
			install( InvocationFault => 
					statusCode = 500
					undef( response )
					response.fault = invoke_scope.InvocationFault.name
					if ( invoke_scope.InvocationFault.name == "TypeMismatch" ) {
						split@StringUtils( invoke_scope.InvocationFault.data { .regex = ":" } )( error_msg )
						response.content = invoke_scope.InvocationFault.name + ":" + error_msg.result[1]
					} else {
						response.content << invoke_scope.InvocationFault.data
					}
					
			)
			invoke@Reflection( invokeReq )( response )

			// default response headers
			responseOutgoingHeaders.("Access-Control-Allow-Methods") = "POST,GET,DELETE,PUT,OPTIONS"
			responseOutgoingHeaders.("Access-Control-Allow-Origin") = "*"
			responseOutgoingHeaders.("Access-Control-Allow-Headers") = "Content-Type"
		}
	}
}

define makeLink
{
	for( i = 0, i < #routes && !found, i++ ) {
		if ( routes[i].method == request.method && routes[i].operation == request.operation ) {
			with( expand ) {
				.template = routes[i].template;
				.params -> request.params
			};
			expand@UriTemplates( expand )( response );
			response = "http://" + config.host + response
		}
	}
}

init {
	config( config )() {
		routes << config.routes
		// normalize route templates
		for( i = 0, i < #routes, i++ ) {
			startsWith@StringUtils( routes[ i ].template { .prefix = "/" } )( start_with_slash )
			if ( !start_with_slash ) {
				routes[ i ].template = "/" + routes[ i ].template
			}
		}
	}
	/*for( r = 0, r < #routes, r++ ) {
			println@Console( "Loaded " + routes[ r ].template )()
	}
	;
	println@Console("Router is running...")()*/
}

main
{
	[ get( request )( response ) {
		method = "get";
		if (HANDLER ){
           headerHandler
		}
		route
		if (HANDLER ){
           headerHandlerResponse
		}
	} ]

	[ post( request )( response ) {
		method = "post";
		if (HANDLER ){
           headerHandler
		}
		route
		if (HANDLER ){
           headerHandlerResponse
		}
	} ]

	[ put( request )( response ) {
		method = "put";
		if (HANDLER ){
           headerHandler
		}
		route
		if (HANDLER ){
           headerHandlerResponse
		}
	} ]

	[ delete( request )( response ) {
		method = "delete";
		if (HANDLER ){
           headerHandler
		}
		route
		if (HANDLER ){
           headerHandlerResponse
		}
	} ]

	[ options( request )( response ) {
		method = "options";
		if (HANDLER){
           headerHandler
		}
		response = ""
		if (HANDLER ){
           headerHandlerResponse
		}
	}]

	[ makeLink( request )( response ) {
		if ( !is_defined( request.method ) ) {
			request.method = "get"
		};
		makeLink
	} ]

}
