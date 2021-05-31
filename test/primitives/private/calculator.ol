type Numbers { x:int y:int }

interface Calculator {
    RequestResponse:
	    add(Numbers)(int),
        mul(Numbers)(int),
        square(int)(int)
    OneWay:
        shutdown(void)
}

inputPort Self {
	Location: "local"
	Interfaces: Calculator
}

execution { concurrent }

main
{
    [
        add( args )( res ) {
            res = args.x + args.y
        } 
    ]
    [
        mul( args )( res ) {
            res = args.x * args.y
        }
    ]
    [
        square( arg )( res ) {
            res = arg * arg
        }
    ]
    [ shutdown() ] { exit }
}
