include "frontend.iol"
include "console.iol"

execution { concurrent }

inputPort JHomeFrontendInput {
Location: "local"
Interfaces: JHomeFrontendInterface
}

main
{
	getPageTitle()( "My jHome page" ) {
		println@Console( "Called" )()
	}
}
