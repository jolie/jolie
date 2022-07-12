from console import Console
from string-utils import StringUtils
from runtime import Runtime
from vectors import Vectors

constants {
	DEFAULT_VECTOR_SIZE = 5000
}

service Main {
	embed Console as console
	embed StringUtils as stringUtils
	embed Runtime as runtime
	embed Vectors as vectors

	main {
		VectorSize =
			if( args[0] instanceof string )
				int( args[0] )
			else
				DEFAULT_VECTOR_SIZE

		println@console(
			"Before vector initialisation:\n" +
			valueToPrettyString@stringUtils(
				stats@runtime()
			)
		)()
		x.items[0] = "0"
		for( i = 1, i < VectorSize, i++ ) {
			add@vectors( {
				vector << &x
				item = string( i )
			} )( x )
		}
		println@console(
			"After vector initialisation:\n" +
			valueToPrettyString@stringUtils(
				stats@runtime()
			)
		)()

		i = 0
		for( item in x.items ) {
			if( item != string( i++ ) ) {
				throw( ItemError, "item " + i + " has value " + item )
			}
		}
		if( i != VectorSize )
			throw( SizeError, i )

		dumpState@runtime()( s )
		// println@console( s )()
	}
}