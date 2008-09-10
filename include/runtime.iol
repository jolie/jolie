outputPort Runtime {
RequestResponse:
	getLocalLocation,
	loadEmbeddedService,
	getRedirection, setRedirection, removeRedirection,
	setOutputPort, removeOutputPort
}

embedded {
Java:
	"joliex.lang.RuntimeService" in Runtime
}