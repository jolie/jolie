include "time.iol"
include "console.iol"

interface tInterface {
	RequestResponse: t
}

outputPort Main {
	Location: "local"
	Interfaces: tInterface
}


inputPort Me {
	Location: "local"
	Interfaces: tInterface
}

embedded {
Jolie:
	"../main.ol" in Main
}

main {
  t()() {
		t@Main()();
		println@Console("supermain")()
	}
}
