include "/types/role_types.iol"

type ParseRoleRequest: void {
  .rolename: string
  .filename: string
}

interface MetaJolieInterface {
RequestResponse:
	parseRoles( ParseRoleRequest)( Role )
}

outputPort MetaJolie {
Interfaces: MetaJolieInterface
}

embedded {
Java:
	"joliex.meta.MetaJolie" in MetaJolie
}
