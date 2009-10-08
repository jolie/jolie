include "../AbstractTestUnit.iol"

define doTest
{
	if (
		"Hello, " + "World!" != "Hello, World!"
		||
		2 + 3 * 5 != 17
	) {
		throw( TestFailed )
	}
}

