include "runtime.iol"


main {
	if ( #args == 0 ) {
		port = "8000"
	} else {
		port = args[ 0 ]
	}
	with( emb ) {
		.filepath = "-C Location=\"socket://localhost:" + port + "\" services/jolietraceviewer/jolietraceviewer_service.ol";
		.type = "Jolie"
	}
	loadEmbeddedService@Runtime( emb )(  )
	while( true ) {
		nullProcess
	}
}