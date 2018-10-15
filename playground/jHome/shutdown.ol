include "config.iol"
include "common/locations.iol"
include "admin.iol"

outputPort GopAdmin {
Location: Location_GopAdmin
Protocol: sodep
Interfaces: GopAdminInterface
}

main
{
	shutdown@GopAdmin()()
}