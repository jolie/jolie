include "../AbstractTestUnit.iol"

define doTest
{
	a[0] = 1;
	a[1] = 2;
	b << a;
	if ( #a != #b ) {
		throw( TestFailed, "vector deep copy cardinality does not match original" )
	}/*;

	c << "Root" { .a = 5, .a.b.c[2] = "Hi", .x = 2 };
	if ( c.a.b.c[2] != "Hi" ) {
		throw( TestFailed, "inline value tree deep copy does not match original" )
	}*/
}

