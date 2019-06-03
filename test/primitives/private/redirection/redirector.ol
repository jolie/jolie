include "locations.iol"
include "console.iol"

include "SumInterface.iol"
include "SubInterface.iol"

outputPort SubService {
Location: Location_Sub
Protocol: http
Interfaces: SubInterface
}

outputPort SumService {
Location: Location_Sum
Protocol: http
Interfaces: SumInterface
}

inputPort Redirector {
Location: Location_Redirector
Protocol: sodep
/* here we define the redirection mapping resource names (Sub and Sum) with existing outputPorts
(SumService and SubService) */
OneWay: shutdown
Redirects:
	Sub => SubService,
	Sum => SumService
}

embedded {
	Jolie:
	  "sum.ol", "sub.ol"
}

main
{
  shutdown()
	exit
}
