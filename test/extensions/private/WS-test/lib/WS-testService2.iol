include "runtime.iol"
include "file.iol"

type RequestType:void {
	.x?:int
	.y?:int
}

type ResponseType:void {
	.return?:int
}

interface CalcService {
	RequestResponse:
	sum( RequestType )( ResponseType ),
	prod( RequestType )( ResponseType )
}

outputPort CalcServicePort {
	Location: "socket://localhost:14000/"
	Protocol: soap {
		.debug=0; .debug.showContent = 1;
		.wsdl = "extensions/private/WS-test/lib/WS-test2.wsdl";
		.wsdl.port = "CalcServicePort"
	}
	Interfaces: CalcService
}

outputPort CalcServiceJoliePort {
	Location: "local"
	Protocol: sodep
	RequestResponse:
		close( void )( void ),
		start( void )( void )
}



define loadLocalService
{
	rq.code = "
	include \"console.iol\"
	type RequestType:void {
		.x?:int
		.y?:int
	}

	type ResponseType:void {
		.return?:int
	}

	interface CalcService {
		RequestResponse:
		sum( RequestType )( ResponseType ),
		prod( RequestType )( ResponseType ),
	}

	execution{ concurrent }

	inputPort SoapPort {
		Location: \"socket://localhost:14000/\"
		Protocol: soap {
			.debug=0;
			.wsdl = \"extensions/private/WS-test/lib/WS-test2.wsdl\";
			.wsdl.port = \"CalcServicePort\"
		}
		Interfaces: CalcService 
	}

	inputPort Local {
		Location: \"local\"
		RequestResponse: close, start
	}

	init {
		start()() { nullProcess }
	}

	main {
		[ close()() {
			nullProcess
		}] { exit }

		[ sum( request )( response ) {
			response.return  = request.x + request.y
		}]

		[ prod( request )( response ) {
			response.return = request.x * request.y
		}]
	}
	
	"
	loadEmbeddedService@Runtime( rq )( CalcServiceJoliePort.location )
	
}
