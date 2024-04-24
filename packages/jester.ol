
/*
The MIT License (MIT)
Copyright (c) 2016 Fabrizio Montesi <famontesi@gmail.com>
Copyright (c) 2016 Claudio Guidi <guidiclaudio@gmail.com>
Copyright (c) 2024 Claudio Guidi <guidiclaudio@gmail.com>

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

from uri-templates import UriTemplates
from reflection import Reflection
from console import Console
from runtime import Runtime
from string-utils import StringUtils

constants {
	HEADER_PORT_OP = "HeaderPort"
}

type Parameters {
        apiRouterHttp: string 
		headerHandlers {
			enable: bool
			headerHandlerService? {
				filename: string 
				requestHeaderOperation: string  
				responseHeaderOperation: string
			}
		}
		targetService << {
			filename: string 
			outputPort << {
				name: string 
				location: string 
			}
		}
        debug: bool {
           showContent: bool
        }
        ssl? {
            keyStore: string
            keyStorePassword: string 
            trustStore: string 
            trustStorePassword: string 
            protocol: string 
        }
}

interface WebIface {
RequestResponse:
	get, post, put, delete, patch, options
}

type ProcessedUrls {
	element_url*: string 
	element_query*: undefined
}

type Route {
	operation: string 
	outputPort: string 
	cast*: undefined 
	parameters*: undefined
}

type FindRouteRequest {
	method: string 
	requestUri: string
}

type MatchTemplateRequest {
	uri_elements: ProcessedUrls
	template_elements: ProcessedUrls
}

type MatchTemplateResponse: bool {
	parameters*: undefined
}

interface RouteUtilsInterface {
	RequestResponse:
		processUrls( string )( ProcessedUrls ),
		findRoute( FindRouteRequest )( Route ) throws RouteNotFound,
		matchTemplate( MatchTemplateRequest )( MatchTemplateResponse )
}


type MakeLinkRequest:void {
	operation:string
	params:undefined
	method?:string // default: get
}

type RouteTemplate:void {
	method:string
	template:string
	operation:string
	cast?:undefined
	outputPort: string
}

type ConfigRouterRequest:void {
	routes*:RouteTemplate
}


interface RouterConfigurationInterface {
RequestResponse:
	config( ConfigRouterRequest )( void )
	makeLink( MakeLinkRequest )( string ) throws BindingNotFound(void)
}


service RouteUtils {

	execution: concurrent

	embed StringUtils as StringUtils

	outputPort RouteUtils {
		location: "local://Utils"
		interfaces: RouteUtilsInterface
	}

	inputPort RouteUtils {
		location: "local://Utils"
		interfaces: RouteUtilsInterface
	}

	

	init {
		config( config )() {
			global.routes << config.routes
			global.host = config.host

			// normalize route templates
			for( i = 0, i < #global.routes, i++ ) {
				startsWith@StringUtils( global.routes[ i ].template { .prefix = "/" } )( start_with_slash )
				if ( !start_with_slash ) {
					global.routes[ i ].template = "/" + global.routes[ i ].template
				}
			}
		}
	}

	main {
		[ processUrls( request )( response ) {
			split@StringUtils( request { regex = "\\?" } )( splitted_path_string )	
			split@StringUtils( splitted_path_string.result[0] { regex = "/" } )( splitted_str );
			response.element_url << splitted_str.result;
			if ( #splitted_path_string.result > 1 ) {
				split@StringUtils(  splitted_path_string.result[ 1 ] { regex = "&" } )( splitted_str );
				for( _s = 0, _s < #splitted_str.result, _s++ ) {
					split@StringUtils( splitted_str.result[ _s ] { regex = "=" } )( splitted_str2 );
					response.element_query.( splitted_str2.result[ 0 ] ).value = splitted_str2.result[ 1 ] 
				}
			}
		}]

		[ findRoute( request )( response ) {
			processUrls@RouteUtils( request )( match_request.uri_elements )
			match_response = false
			for( i = 0, i < #global.routes && !match_response, i++ ) {
				if ( global.routes[ i ].method == request.method ) {
					processUrls@RouteUtils( global.routes[ i ].template )( match_request.template_elements )
					matchTemplate@RouteUtils( match_request )( match_response )
				}
			}
			if ( match_response ) {
				response << {
					operation = global.routes[ i ].operation;
					outputPort = global.routes[ i ].outputPort;
					cast << global.routes[ i ].cast
				}
			} else {
				throw( RouteNotFound )
			}
		}]

		[ matchTemplate( request ) ( response ) {
			template_elements -> request.template_elements
			uri_elements -> request.uri_elements
			found = false;

			if ( #template_elements.element_url == #uri_elements.element_url ) {
				found = true
				elementIndex = 0;
				while( found && elementIndex < #uri_elements.element_url ) {
					// check if elements are equal, in case of parameters the value must be extracted
					if ( uri_elements.element_url[ elementIndex ] != template_elements.element_url[ elementIndex ] ) {
							// verify if there is a parameter in the template
							w = template_elements.element_url[ elementIndex ];
							w.regex = "\\{(.*)\\}";
							find@StringUtils( w )( is_param );
							if ( is_param == 0 ) {
									// in this case there is no match
									found = false
							} else {
									// found a parameter
									response.parameters.( is_param.group[ 1 ] ) = uri_elements.element_url[ elementIndex ]
							}
					}
					elementIndex++
				}
			}

			if ( found ) {
				// if the template matches, the query paramters are processed
				foreach ( query_element : uri_elements.element_query ) {
					if ( !is_defined( template_elements.element_query.( query_element ) ) ) {
						// if there is no corrispondance with query parameters, the match fails
						found = false
					} else {
						w = template_elements.element_query.( query_element ).value;
						w.regex = "\\{(.*)\\}";
						find@StringUtils( w )( is_param );
						if ( is_param == 0 ) {
							found = false
						} else {
							// check if an array has been passed, accepted a list of value separated by comma
							split@StringUtils( uri_elements.element_query.( query_element ).value { regex = "," } )( splt )
							for( itemIndex = 0, itemIndex < #splt.result, itemIndex++ ) {
								response.parameters.( is_param.group[ 1 ] )[ itemIndex ] = splt.result[ itemIndex ]
							}
						}
					}
				} 
			}
			response = found
		}]

		[ makeLink( request )( response ) {
			if ( !is_defined( request.method ) ) { request.method = "get" }
			found = false
			for( i = 0, i < #global.routes && !found, i++ ) {
				if ( global.routes[ i ].method == request.method && global.routes[ i ].operation == request.operation ) {
					found = true
					with( expand ) {
						template = global.routes[ i ].template;
						params -> request.params
					};
					expand@UriTemplates( expand )( response );
					response = "http://" + config.host + response
				}
			}
			if ( !found ) {
				throw( RouteNotFound )
			}
		} ]
	
	}
}


service Main ( p : Parameters ) {

    execution: concurrent  

	embed Runtime as Runtime
	embed StringUtils as StringUtils
	embed Reflection as Reflection
	embed UriTemplates as UriTemplates
	embed Console as Console
	embed RouteUtils

	outputPort RouteUtils {
		location: "local://Utils"
		interfaces: RouteUtilsInterface
	}

	outputPort RouteConfigurationUtils {
		location: "local://Utils"
		interfaces: RouterConfigurationInterface
	}

	inputPort RouterAdminPort {
		location: "local://RouterAdminPort"
		aggregates: RouteConfigurationUtils
	}

    inputPort WebInput {
        location: p.apiRouterHttp
        protocol: http {
            debug << p.debug
            default << {
                get = "get"
                post = "post"
                put = "put"
                delete = "delete"
                patch = "patch"
                options = "options"
            }
            method -> method
            headers.authorization = "authorization"
            response.headers -> responseOutgoingHeaders
            statusCode -> statusCode
            format = "json"
        }
        interfaces: WebIface
    }

    inputPort WebInputHttps {
        location: p.apiRouterHttp
        protocol: https {
            debug << p.debug
            default << {
                get = "get"
                post = "post"
                put = "put"
                delete = "delete"
                patch = "patch"
                options = "options"
            }
            method -> method;
            headers.authorization = "authorization"
            response.headers -> responseOutgoingHeaders
            statusCode -> statusCode;
            ssl << {
                keyStore = p.ssl.keyStore
                keyStorePassword = p.ssl.keyStorePassword
                trustStore = p.ssl.trustStore
                trustStorePassword = p.ssl.trustStorePassword
                protocol = p.ssl.protocol
            }
            format = "json"
        }
        interfaces: WebIface
    }



	define _hadlingRequestHeaders {
		// get the service invocation parameters from the header
		headerHandlerRequest <<  {
			data << {
				operation = method
				data.headers << request
			}
			operation = p.headerHandlers.headerHandlerService.requestHeaderOperation
			outputPort = HEADER_PORT_OP
		}
		invoke@Reflection( headerHandlerRequest )( headerHandlerResponse )
		undef ( request.authorization ) 
		foreach( n : headerHandlerResponse ) {
			targetServiceInvokeRequest.data.(n) << headerHandlerResponse.(n)
		}
	}

	define _hadlingResponseHeaders {
   		undef( headerHandlerRequest )
		headerHandlerRequest << {
			data << {
				operation = method
				response << response
			}
			operation =  p.headerHandlers.headerHandlerService.responseHeaderOperation
			outputPort = HEADER_PORT_OP
		}
		invoke@Reflection( headerHandlerRequest )( headerHandlerResponse ) 
   		foreach( n : headerHandlerResponse ) {
	       if ( is_defined( headerHandlerResponse.(n).deleteResponseNode ) ){
				// deleting default if necessary
			    undef ( response.( headerHandlerResponse.(n).deleteResponseNode ) )
		   }
		   responseOutgoingHeaders.(n) = headerHandlerResponse.(n)
		}
	}

	define _route	{
		// find a route starting from the request
		statusCode = 200
		scope( processingRoute ) {
			install( RouteNotFound => statusCode = 404 )
			findRoute@RouteUtils( request.requestUri )( foundRoute )
			if ( p.headerHandlers.enable ){ _hadlingRequestHeaders }
		
			targetServiceInvokeRequest << {
				.operation = foundRoute.operation;
				.outputPort = foundRouteoutputPort
			}
			// populate request to target service extracting the request body
			foreach( d : request.data ) {
				targetServiceInvokeRequest.data.( d ) << request.data.( d )
			};	

			// adding extra request fields from url parameters
			foreach( p : foundRoute.parameters ) {
				if ( is_defined( foundRoute.cast.( p ) ) ) {
					// in case of a casting is required 
					if ( foundRoute.cast.( p ) == "int" ) {
							for( counter = 0, counter < #foundRoute.parameters.( p ) , counter++ ) {
								targetServiceInvokeRequest.data.( p )[ counter ] = int( foundRoute.parameters.( p )[ counter ] )
							}
					} else if ( cast.( n ) == "long" ) {
							for( counter = 0, counter < #foundRoute.parameters.( p ) , counter++ ) {
								targetServiceInvokeRequest.data.( p )[ counter ] = long( foundRoute.parameters.( p )[ counter ] )
							}
					} else if ( cast.( n ) == "double" ) {
							for( counter = 0, counter < #foundRoute.parameters.( p ) , counter++ ) {
								targetServiceInvokeRequest.data.( p )[ counter ] = double( foundRoute.parameters.( p )[ counter ] )
							}
					} else if ( cast.( n ) == "bool" ) {
							for( counter = 0, counter < #foundRoute.parameters.( p ) , counter++ ) {
								targetServiceInvokeRequest.data.( p )[ counter ] = bool( foundRoute.parameters.( p )[ counter ] )
							}
					}
				} else {
					/* all the other cases */
					targetServiceInvokeRequest.data.( p ) << foundRoute.parameters.( p )		
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
				invoke@Reflection( targetServiceInvokeRequest )( response )
			}
		}

		// handling response headers
		if ( p.headerHandlers.enable ){ _hadlingResponseHeaders }
	}

	init {
		enableTimestamp@Console( true )()
		

		// check if header handler is enabled and embed the service if true
		if ( p.headerHandlers.enable ) {
			
			emb << {
				filepath = p.headerHandlers.headerHandlerService.filename
				type = "Jolie"
			}
			loadEmbeddedService@Runtime( emb )( headerHandlerServiceLocation )
			set_op << {
					name =  HEADER_PORT_OP
					location = headerHandlerServiceLocation
					protocol = "sodep"
			}
			setOutputPort@Runtime( set_op )()
			println@Console( "Header Handler Service embedded" )()
		}

		// embedding target service
		emb << {
			filepath = p.targetService.filename
			type = "Jolie"
		}
		loadEmbeddedService@Runtime( emb )( targetServiceLocation )
		set_op << {
				name =  p.targetServiceLocation.outputPort.name 
				location = p.targetServiceLocation.outputPort.location 
				protocol = "sodep"
		}
		setOutputPort@Runtime( set_op )()
		println@Console( "Target Service embedded" )()

		// default response headers
		responseOutgoingHeaders.("Access-Control-Allow-Methods") = "POST,GET,DELETE,PUT,PATCH,OPTIONS"
		responseOutgoingHeaders.("Access-Control-Allow-Origin") = "*"
		responseOutgoingHeaders.("Access-Control-Allow-Headers") = "Content-Type"
	}

	main {
		[ get( request )( response ) {
			method = "get"
			_route
		} ]

		[ post( request )( response ) {
			method = "post"
			_route
		} ]

		[ put( request )( response ) {
			method = "put"
			_route
		} ]

		[ delete( request )( response ) {
			method = "delete"
			_route
		} ]

		[ options( request )( response ) {
			method = "options"
			_route
		}]

		[ patch( request )( response ) {
			method = "patch";
			_route
		}]
	}

}

















