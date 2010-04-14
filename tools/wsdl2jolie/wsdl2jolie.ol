include "web_services_utils.iol"
include "console.iol"

main
{
	if ( #args != 1 ) {
		println@Console( "Syntax is wsdl2jolie <URL to WSDL>" )()
	} else {
		wsdlToJolie@WebServicesUtils( args[0] )( result );
		println@Console( result )()
	}
}