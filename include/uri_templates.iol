type UriMatchRequest:void {
	.uri:string
	.template:string
}

type MatchResponse:bool { ? }

type ExpandRequest:void {
	.template:string
	.params?:undefined
}

/**!
WARNING: the API of this service is experimental. Use it at your own risk.
*/
interface UriTemplatesIface {
RequestResponse:
	match(UriMatchRequest)(MatchResponse),
	expand(ExpandRequest)(string)
}

outputPort UriTemplates {
Interfaces: UriTemplatesIface
}

embedded {
Java:
	"joliex.util.UriTemplates" in UriTemplates
}
