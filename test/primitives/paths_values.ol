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

define testPval
{
	// Setup test data with children
	mydata[0] = 100;
	mydata[0].child = "zero";
	mydata[1] = 200;
	mydata[1].child = "one";
	mydata[2] = 300;
	mydata[2].child = "two";

	// Get paths to elements > 150
	res << paths mydata[*] where $ > 150;

	if ( #res.results != 2 ) {
		throw( TestFailed, "Expected 2 paths, got " + #res.results )
	};

	// Test pval as rvalue (reading root value)
	readVal = pval(res.results[0]);
	if ( readVal != 200 ) {
		throw( TestFailed, "Expected pval(res.results[0]) to return 200, got " + readVal )
	};

	// Test pval with path navigation - pval(x).child
	childVal = pval(res.results[0]).child;
	if ( childVal != "one" ) {
		throw( TestFailed, "Expected pval(res.results[0]).child to be 'one', got '" + childVal + "'" )
	};

	childVal2 = pval(res.results[1]).child;
	if ( childVal2 != "two" ) {
		throw( TestFailed, "Expected pval(res.results[1]).child to be 'two', got '" + childVal2 + "'" )
	};

	// Test pval with array indexing - pval(x).children[n]
	mydata[1].children[0] = "first";
	mydata[1].children[1] = "second";
	mydata[1].children[2] = "third";

	arrayVal = pval(res.results[0]).children[1];
	if ( arrayVal != "second" ) {
		throw( TestFailed, "Expected pval(res.results[0]).children[1] to be 'second', got '" + arrayVal + "'" )
	};

	// Test pval with composite path - pval(x).a.b[n].c
	mydata[1].nested.items[0].name = "item0";
	mydata[1].nested.items[1].name = "item1";
	mydata[1].nested.items[2].name = "item2";

	compositeVal = pval(res.results[0]).nested.items[1].name;
	if ( compositeVal != "item1" ) {
		throw( TestFailed, "Expected pval(res.results[0]).nested.items[1].name to be 'item1', got '" + compositeVal + "'" )
	}
}

define doTest
{
	testPathsBasic;
	testPathType;
	testNestedFieldAccess;
	testPval
}
