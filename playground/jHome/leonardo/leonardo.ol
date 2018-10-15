include "console.iol"
include "file.iol"
include "string_utils.iol"
include "../frontend/frontend.iol"
include "../backend/backend.iol"

include "config.iol"

execution { concurrent }

interface HTTPInterface {
RequestResponse:
	default(undefined)(undefined)
}

outputPort Frontend {
Interfaces: JHomeFrontendInterface
}
outputPort Backend {
Interfaces: JHomeBackendInterface
}

embedded {
Jolie:
	"../frontend/frontend.ol" in Frontend,
	"../backend/backend.ol" in Backend
}

include "../services/news/embed.iol"

inputPort HTTPInput {
Protocol: http {
	.keepAlive = 0; // Do not keep connections open
	.debug = DebugHttp; 
	.debug.showContent = DebugHttpContent;
	.format -> format;
	.contentType -> mime;

	.default = "default"
}
Location: Location_Leonardo
Interfaces: HTTPInterface
Aggregates: Frontend, Backend
Redirects: News => News
}

init
{
	format = "json";
	documentRootDirectory = args[0]
}

main
{
	// Do _not_ modify the behaviour of the default operation.
	[ default( request )( response ) {
		scope( s ) {
			install( FileNotFound => println@Console( "File not found: " + file.filename )() );

			if ( request.operation == "" ) {
				request.operation = "pages/home"
			};
			s = request.operation;
			s.regex = "/";
			s.limit = 2;
			split@StringUtils( s )( s );
			if ( s.result[0] == "pages" ) { // It's a dynamic jHome page
				file.format = "text";
				format = "html";
				scope( s1 ) {
					install( PageNotFound => response = "404 - Page not found" );
					getPage@Frontend( s.result[1] )( response )
				}
			} else if ( s.result[0] == "admin" ) {
				file.format = "text";
				format = "html";
				scope( s2 ) {
					install( PageTemplateNotFound => response = "500 - Page template not found" );
					getPageTemplate@Backend( s.result[1] )( response )
				}				
			} else { // It's a static file
				undef( s );
				s = request.operation;
				s.regex = "\\?";
				split@StringUtils( s )( s );
				file.filename = documentRootDirectory + s.result[0];
				getMimeType@File( file.filename )( mime );
				mime.regex = "/";
				split@StringUtils( mime )( s );
				if ( s.result[0] == "text" ) {
					file.format = "text";
					format = "html"
				} else {
					file.format = format = "binary"
				};

				readFile@File( file )( response )
			}
		}
	} ] { nullProcess }
}
