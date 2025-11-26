include "../AbstractTestUnit.iol"

type PathResult {
	results[0,*]: path
}

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

define testPathType
{
	data[0] = 100;
	data[1] = 200;
	data[2] = 300;

	result << paths data[*] where $ > 150;

	// Verify we get path type values
	if ( #result.results != 2 ) {
		throw( TestFailed, "Expected 2 path results, got " + #result.results )
	};

	// Verify path values are of type path using instanceof
	if ( !(result.results[0] instanceof path) ) {
		throw( TestFailed, "Expected result.results[0] to be instanceof path" )
	};

	if ( !(result.results[1] instanceof path) ) {
		throw( TestFailed, "Expected result.results[1] to be instanceof path" )
	};

	// Verify path values can be converted to string
	pathStr = "" + result.results[0];
	if ( pathStr != "data[1]" ) {
		throw( TestFailed, "Expected path 'data[1]', got '" + pathStr + "'" )
	};

	// Verify result matches PathResult type structure
	if ( !(result instanceof PathResult) ) {
		throw( TestFailed, "Expected result to be instanceof PathResult" )
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
	testPathType;
	testNestedFieldAccess
}
