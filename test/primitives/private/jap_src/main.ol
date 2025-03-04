// Test path resolution inside of JAPs
include "twice/d1/d2/twice_api.iol"

inputPort TwiceInput {
location: "local"
interfaces: TwiceAPI
}

main
{
	twice( x )( x * Factor )
}