include "common/locations.iol"
include "admin.iol"

embedded {
Jolie:
	"database/database.ol",
	"frontend/frontend.ol",
	"backend/backend.ol",
	"leonardo/leonardo.ol www/"
}

inputPort JHomeAdminInput {
Location: Location_JHomeAdmin
Protocol: sodep
Interfaces: JHomeAdminInterface
}

main
{
	shutdown()() {
		nullProcess
	}
}
