include "twice_api.iol"

inputPort TwiceInput {
location: "local"
interfaces: TwiceAPI
}

main
{
	twice( x )( x * 2 )
}