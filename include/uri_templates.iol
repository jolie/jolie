type MatchRequest:void {
	.uri:string
	.template:string
}

type MatchResponse:bool { ? }

/**!
WARNING: the API of this service is experimental. Use it at your own risk.
*/
interface UriTemplatesIface {
RequestResponse:
	match(MatchRequest)(MatchResponse)
}

outputPort UriTemplates {
Interfaces: UriTemplatesIface
}

embedded {
Java:
	"joliex.util.UriTemplates" in UriTemplates
}
