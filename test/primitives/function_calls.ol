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

	d = add@Calculator( {x = 10, y = 10} ) - mul@Calculator( {x = 2, y = 5} )
	e = add@Calculator( {x = 10, y = 10} ) / mul@Calculator( {x = 2, y = 5} )
	if ( d != 10 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( e != 2 ) {
		throw( TestFailed, "Unexpected result" )
	}

	f = square@Calculator( square@Calculator( 3 ) )
	g = square@Calculator( 5 ) != square@Calculator( 5 )
	h = square@Calculator( add@Calculator( {x = 1, y = 4} ) ) >= square@Calculator( mul@Calculator( {x = 2, y = 2} ) ) 
	if ( f != 81 ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( g ) {
		throw( TestFailed, "Unexpected result" )
	}
	if ( !h ) {
		throw( TestFailed, "Unexpected result" )
	}

	shutdown@Calculator()
}
