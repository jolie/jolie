

include "console.iol"
include "file.iol"
include "string_utils.iol"
include "runtime.iol"
include "protocols/http.iol"

include "FrontendInterface.iol"

constants {

	// The default page to serve in case clients do not specify one
	DefaultPage = "index.html",

	// Print debug messages for all exchanged HTTP messages
	DebugHttp = false,

	// Add the content of every HTTP message to their debug messages
	DebugHttpContent = false
}


execution { concurrent }

outputPort Frontend {
	Interfaces: FrontendInterface
}

embedded {
	Jolie: "frontend.ol" in Frontend

}

interface HTTPInterface {
RequestResponse:
	default(DefaultOperationHttpRequest)(undefined)
}

inputPort HTTPInput {
Protocol: http {
	.keepAlive = true; // Keep connections open
	.debug = DebugHttp;
	.debug.showContent = DebugHttpContent;
	.format -> format;
	.contentType -> mime;
	.statusCode -> statusCode;

	.default = "default"
}
Location: Location
Interfaces: HTTPInterface
Aggregates: Frontend
}

init
{
	getenv@Runtime( "JOLIE_HOME" )( JOLIE_HOME )
	RootContentDirectory = JOLIE_HOME + "/include/services/jolietraceviewer/www/"
	if ( is_defined( args[0] ) ) {
		documentRootDirectory = args[0]
	} else {
		documentRootDirectory = RootContentDirectory
	}
	replaceAll@StringUtils( Location { .regex="socket", .replacement="http"} )( http_location )
	println@Console("Jolie Trace Viewer is running, open your browser and set the url " + http_location )()
}

main
{
	[ default( request )( response ) {
		scope( s ) {
			install( FileNotFound => nullProcess; statusCode = 404 );

			split@StringUtils( request.operation { .regex = "\\?" } )( s );

			// Default page
			shouldAddIndex = false;
			if ( s.result[0] == "" ) {
				shouldAddIndex = true
			} else {
				endsWith@StringUtils( s.result[0] { .suffix = "/" } )( shouldAddIndex )
			};
			if ( shouldAddIndex ) {
				s.result[0] += DefaultPage
			};

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
	} ] { nullProcess }
}
