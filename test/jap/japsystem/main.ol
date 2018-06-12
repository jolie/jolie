include "jap:file:sample.jap!/public/interfaces/SampleInterface.iol"
include "time.iol"
include "console.iol"


interface tInterface {
	RequestResponse: t
}

inputPort Me {
	Location: "local"
	Interfaces: tInterface
}

embedded {
Jolie:
	"sample.jap"
}

main {
    t()(){ println@Console("main")() }
}
