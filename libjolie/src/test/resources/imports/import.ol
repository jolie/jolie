from .private.imports.point import point
include "../AbstractTestUnit.iol"

define doTest
{
    v << {x= 1, y= 2}

	if ( !(v instanceof point) ) {
		throw( TestFailed, "point is not imported" )
	}
}