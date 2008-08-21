outputPort Runtime {
RequestResponse:
	getLocalLocation,
	loadEmbeddedService,
	getRedirection, setRedirection,
	setOutputPort
}

embedded {
Java:
	"joliex.lang.RuntimeService" in Runtime
}