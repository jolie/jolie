include "../AbstractTestUnit.iol"

define doTest
{
	greetings << {
		.Hi = false,
		.Hello = false,
		.Howdy = false
	};

	i = 0;
	foreach( k : greetings ) {
		greetings.(k) = true;
		i++
	};

	if ( !(greetings.Hi && greetings.Hello && greetings.Howdy) ) {
		throw( TestFailed, "greetings do not match (foreach test)" )
	};

	if ( i != 3 ) {
		throw( TestFailed, "greetings size does not match (foreach test)" )
	}
}
