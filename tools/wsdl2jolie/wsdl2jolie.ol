include "web_services_utils.iol"
include "console.iol"
include "file.iol"

main
{
	if ( #args != 1 && # args != 2 ) {
		println@Console( "Syntax is wsdl2jolie <URL to WSDL> [filename]" )()
	} else {
		wsdlToJolie@WebServicesUtils( args[0] )( result );
		if ( #args == 2 ) {
			file.filename = args[ 1 ];
			file.content = result;
			writeFile@File( file )()
		} else {
			println@Console( result )()
		}
	}
}
