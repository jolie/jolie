type MatchRequest:void {
	.uri:string
	.template:string
}

/**!
WARNING: the API of this service is experimental. Use it at your own risk.
*/
interface UriTemplatesIface {
RequestResponse:
	match(MatchRequest)(undefined)
}

outputPort UriTemplates {
Interfaces: UriTemplatesIface
}

embedded {
Java:
	"joliex.util.UriTemplates" in UriTemplates
}
