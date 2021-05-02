include "../AbstractTestUnit.iol"

type Numbers { x:int y:int }

interface Calculator {
    RequestResponse:
	    add(Numbers)(int),
        mul(Numbers)(int),
        square(int)(int)
    OneWay:
        shutdown(void)
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
	c = add@Calculator( {x = a, y = b} )
    if ( a != 3 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( b != 6 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( c != 9 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( ( add@Calculator( {x = 10, y = 10} ) - mul@Calculator( {x = 2, y = 5} ) ) != 10 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( 2 != ( add@Calculator( {x = 10, y = 10} ) / mul@Calculator( {x = 2, y = 5} ) ) ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( square@Calculator( square@Calculator( 3 ) ) != 81 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( square@Calculator( 5 ) != square@Calculator( 5 ) ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( !( square@Calculator( 5 ) != square@Calculator( 5 ) ) ) {
		throw( TestFailed, "Unexpected result" )
	}
	shutdown@Calculator()
}
