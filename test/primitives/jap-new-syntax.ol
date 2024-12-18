from ..test-unit import TestUnitInterface
from .private.twice-new-syntax.twice.d1.d2.twice_api import TwiceAPI
from .private.twice-new-syntax.main import test


service TEST {

	embed test as Twice1
	embed test as Twice2

	outputPort Twice1 { interfaces: TwiceAPI }
	outputPort Twice2 { interfaces: TwiceAPI }


	define doTest
	{
		twice@Twice1( 2 )( x )
		if ( x != 4 ) throw( TestFailed, "expected 4, received " + x )
		twice@Twice2( 2 )( x )
		if ( x != 4 ) throw( TestFailed, "expected 4, received " + x )
	}
}
