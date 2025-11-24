include "../AbstractTestUnit.iol"

define testPathsBasic
{
	data[0] = 10;
	data[1] = 20;
	data[2] = 30;

	result << paths data[*] where $ > 15;

	if ( #result.results != 2 ) {
		throw( TestFailed, "Expected 2 results, got " + #result.results )
	}
}

define testNestedFieldAccess
{
	root.items[0].value = 5;
	root.items[1].value = 15;
	root.items[2].value = 25;

	// Test navigation to items[*].value
	result << paths root.items[*].value where $ > 10;

	if ( #result.results != 2 ) {
		throw( TestFailed, "Expected 2 items with value>10, got " + #result.results )
	}
}

define doTest
{
	testPathsBasic;
	testNestedFieldAccess
}
