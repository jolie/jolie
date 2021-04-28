include "../AbstractTestUnit.iol"

type Numbers { x:int y:int }

interface Calculator {
RequestResponse:
	add(Numbers)(int),
    mul(Numbers)(int)
}

outputPort Calculator {
	Interfaces: Calculator
}

embedded {
Jolie: "private/calculator.ol" in Calculator
}

define doTest
{
    a = add@Calculator( {x = 1, y = 2} )
	b = mul@Calculator( {x = 2, y = 3} )
    if ( a != 3 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( b != 6 ) {
		throw( TestFailed, "Unexpected result" )
	}
}
