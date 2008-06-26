outputPort Runtime {
SolicitResponse:
	loadEmbeddedService, getRedirection, setRedirection, setOutputPort
}

embedded {
Java:
	"joliex.lang.RuntimeService" in Runtime
}