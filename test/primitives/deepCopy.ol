include "../AbstractTestUnit.iol"

define doTest
{
	a[0] = 1;
	a[1] = 2;
	b << a;
	if ( #a != #b ) {
		throw( TestFailed, "vector deep copy cardinality does not match original" )
	}
}

