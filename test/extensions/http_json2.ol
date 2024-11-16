include "json_utils.iol"


type Response1: B | A
type Response2: A | B

type A {
    canbeanything*: undefined
    test: string
}

type B {
    test?: A
}

interface Iface {
    RequestResponse: AB(void)(Response1)
	RequestResponse: BA(void)(Response2)
}

define makeResponse
{
	Response << {
		test << {
			canbeanything[0] = "This should be wrapped in an array"
			test = ""
			//Not wrapped because something with the same name (in this case "test") exists in both of the types (we think).
			/*
			curl localhost:12345/hello
			{"test":{"test":"","canbeanything":"This should be wrapped in an array"}}
			*/
		}
	}
} 


service Bug {
    execution: concurrent
    inputPort ip {
        location: "socket://localhost:12345"
        protocol: http {
            format = "json"
		    .contentType = "text/plain"
        }
        interfaces: Iface
    }
    main {
        [AB()(Response) {
            makeResponse
         }]
		[BA()(Response) {
            makeResponse
         }] 
    }
}

