type Numbers { x:int y:int }

interface Calculator {
    RequestResponse:
	    add(Numbers)(int),
        mul(Numbers)(int)
}

inputPort Self {
	Location: "local"
	Interfaces: Calculator
}

execution { single }

main
{
    add( args )( res ) {
        res = args.x + args.y
    }
    mul( args )( res ) {
        res = args.x * args.y
    }
}
