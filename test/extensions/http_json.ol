include "../AbstractTestUnit.iol"

from .http_json2 import Iface

interface Iface2 {
    RequestResponse: AB(void)(string)
	RequestResponse: BA(void)(string)
}

outputPort Server {
	Location: "socket://localhost:12345"
	Protocol: http {
		format = "json"
	}
	Interfaces: Iface2
}

embedded {
Jolie:
	"http_json2.ol"
}


define doTest
{
	AB@Server()( thing1 )
	BA@Server()( thing2 )

	if ( thing1 ==  thing2) {
		t = "yes"
	} else {
		t = "not"
	}
	a = "{\"test\":{\"test\":\"\",\"canbeanything\":\"This should be wrapped in an array\"}}"
	throw( TestFailed, "\n" + thing1 + "\n" + thing2 + "\n" + t )
}
