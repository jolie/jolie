outputPort Runtime {
RequestResponse:
	getLocalLocation,
	loadEmbeddedService,
	getRedirection, setRedirection, removeRedirection,
	setOutputPort, removeOutputPort, callExit
}

embedded {
Java:
	"joliex.lang.RuntimeService" in Runtime
}