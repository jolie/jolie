include "../AbstractTestUnit.iol"

define terminationTest
{
	i = y = 0;
	while( i++ < 5 ) {
		scope( m ) {
			install( MyFault => comp( s2 ) );
			{
				scope( s1 ) {
					throw( MyFault )
				}
				|
				scope( s2 ) {
					install( this => y++ );
					nullProcess
				}
			}
		}
	};
	if ( y != i - 1 ) {
		throw( TestFailed, "termination/compensation handling in parallel scopes did not work" )
	}
}

define simpleFaultTest
{
	scope( s ) {
		install(
			MyFault => x = 1
		);
		throw( MyFault );
		x = 5
	};
	if ( x != 1 ) {
		throw( TestFailed, "an installed fault handler was not executed" )
	}
}

define doTest
{
	simpleFaultTest;
	terminationTest
}

