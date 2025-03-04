from ..test-unit import TestUnitInterface
from twice-new-syntax.twice.d1.d2.twice_api import TwiceAPI
from twice-new-syntax.main import test

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed test as Twice1
	embed test as Twice2

	main
	{
		test()() {
			twice@Twice1( 2 )( x )
			if ( x != 4 ) throw( TestFailed, "expected 4, received " + x )
			twice@Twice2( 2 )( x )
			if ( x != 4 ) throw( TestFailed, "expected 4, received " + x )
		}
	}
}
