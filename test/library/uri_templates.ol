include "../AbstractTestUnit.iol"
include "uri_templates.iol"

define doTest
{
	match@UriTemplates( {
		.uri = "/chat/5/history?format=json&length=10",
		.template = "/chat/{id}/history?format={format}&length={length}"
	} )( result );
	if ( result.id != "5" ) {
		throw( TestFailed, "Uri template matching result (" + result.id + ") does not contain expected value (5)" )
	}
}
