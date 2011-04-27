include "console.iol"
include "file.iol"
include "string_utils.iol"

include "config.iol"

execution { concurrent }

interface HTTPInterface {
RequestResponse:
	default(undefined)(undefined)
}

outputPort Frontend {
Interfaces: JHomeFrontendInterface
}

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
}

init
{
	documentRootDirectory = args[0]
}

main
{
	// Do _not_ modify the behaviour of the default operation.
	[ default( request )( response ) {
		scope( s ) {
			install( FileNotFound => println@Console( "File not found: " + file.filename )() );

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
	} ] { nullProcess }
}
