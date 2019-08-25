
/*
The MIT License (MIT)
Copyright (c) 2016 Fabrizio Montesi <famontesi@gmail.com>
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

inputPort WebInput {
Location: API_ROUTER
Protocol: http {
	.debug=true;.debug.showContent=true;
	.default.get = "get";
	.default.post = "post";
	.default.put = "put";
	.default.delete = "delete";
	.default.options = "options";
	.method -> method;
	.response.headers.("Access-Control-Allow-Methods") = "POST,GET,DELETE,PUT,OPTIONS";
	.response.headers.("Access-Control-Allow-Origin") = "*";
	.response.headers.("Access-Control-Allow-Headers") = "Content-Type";
	.statusCode -> statusCode;
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
  split_str.regex = "/";
  split@StringUtils( split_str )( splitted_str );
  __str_elements.element << splitted_str.result;
  split_str = splitted_str.result[ #splitted_str.result - 1 ];
  split_str.regex = "\\?";
  split@StringUtils( split_str )( splitted_str );
  if ( #splitted_str.result > 1 ) {
      __str_elements.element[ #__str_elements.element - 1] = splitted_str.result [ 0 ];
      split_str = splitted_str.result[ 1 ];
      split_str.regex = "&";
      split@StringUtils( split_str )( splitted_str );
      for( _s = 0, _s < #splitted_str.result, _s++ ) {
          split_str2 = splitted_str.result[ _s ];
          split_str2.regex = "=";
          split@StringUtils( split_str2 )( splitted_str2 );
          __str_elements.element[ #__str_elements.element ]  = splitted_str2.result[ 1 ]
      }
  }
}

define matchTemplate {
	/* __input_str */

  undef( template_elements );
	split_urls;
	template_elements << __str_elements;
	found = false;
	if ( #template_elements.element == #uri_elements.element ) {
		found = true;
		_e = 0;
		while( found && _e < #uri_elements.element ) {
				if ( uri_elements.element[ _e ] != template_elements.element[ _e ] ) {
						w = template_elements.element[ _e ];
						w.regex = "\\{(.*)\\}";
						find@StringUtils( w )( is_param );
						if ( is_param == 0 ) {
								found = false
						} else {
								found.( is_param.group[1] ) = uri_elements.element[ _e ]
						}
				}
				;
				_e++
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
						invokeReq.data.(n) = int( found.(n) )
					} else if ( cast.( n ) == "long" ) {
						invokeReq.data.(n) = long( found.(n) )
					} else if ( cast.( n ) == "double" ) {
						invokeReq.data.(n) = double( found.(n) )
					} else if ( cast.( n ) == "bool" ) {
						invokeReq.data.(n) = bool( found.(n) )
					}
			} else {
				  /* all the other cases */
					invokeReq.data.(n) << found.(n)
		  }
		};

		invoke@Reflection( invokeReq )( response )
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
	for( r = 0, r < #routes, r++ ) {
			println@Console( "Loaded " + routes[ r ].template )()
	}
	;
	println@Console("Router is running...")()
}

main
{
	[ get( request )( response ) {
		method = "get";
		route
	} ]

	[ post( request )( response ) {
		method = "post";
		route
	} ]

	[ put( request )( response ) {
		method = "put";
		route
	} ]

	[ delete( request )( response ) {
		method = "delete";
		route
	} ]

	[ options( request )( response ) {
		response = ""
	}]

	[ makeLink( request )( response ) {
		if ( !is_defined( request.method ) ) {
			request.method = "get"
		};
		makeLink
	} ]

}
