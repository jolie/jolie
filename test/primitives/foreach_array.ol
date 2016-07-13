include "../AbstractTestUnit.iol"

define doTest
{
	animals << {
		.pets[0] = "cat",
		.pets[1] = "dog",
		.pets[2] = "parrot"
	};

	numbers[0] = 7;
	numbers[1] = 3;
	numbers[2] = 10;

  i = 0;
	foreach( k -> animals.pets ) {
		if( k != animals.pets[i++] ) {
			throw( TestFailed, "foreach doesn't work with subnodes (item comparison)" )
		}
	};

  if( i != 3 ) {
		throw( TestFailed, "foreach doesn't work with subnodes (index)" )
	};

  i = 0;
	foreach( k -> numbers ) {
		if( k != numbers[i++] ) {
			throw( TestFailed, "foreach doesn't work with root nodes" )
		}
	};

	if( i != 3 ) {
    throw( TestFailed, "foreach doesn't work with root nodes" )
	};

  foreach( k -> emptyArray ) {
		throw( TestFailed, "foreach doesn't work with empty nodes" )
	};

  i = 0;
	foreach( k[3] -> animals.pets ) {
		if( k[3] != animals.pets[i++] ) {
			throw( TestFailed, "foreach doesn't work with indexed key paths (item comparison)" )
		}
	};

	if( i != 3 ) {
		throw( TestFailed, "foreach doesn't work with indexed key paths (index)" )
	};

  i = 0;
	foreach( k[33] -> numbers ) {
		if( k[33] != numbers[i++] ) {
			throw( TestFailed, "foreach doesn't work with indexed key paths and root nodes (item comparison)" )
		}
	};

	if( i != 3 ) {
    throw( TestFailed, "foreach doesn't work with indexed key paths and root nodes (index)" )
	};

  foreach( k[33] -> emptyArray ) {
		throw( TestFailed, "foreach doesn't work with indexed key paths and empty nodes" )
	}
}
