include "../AbstractTestUnit.iol"

define doTest
{
	if ( "Hello, " + "World!" != "Hello, World!" ) {
		throw( TestFailed, "string concatenation does not match correct result" )
	} else if ( 17 != 2 + 3 * 5 ) {
		throw( TestFailed, "arithmetic expression does not match correct result" )
	}
}

