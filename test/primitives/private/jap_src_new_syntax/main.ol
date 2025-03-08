from .twice.d1.d2.twice_api import TwiceAPI

constants {
	Factor = 2
}


service test {

	execution: concurrent



	inputPort TwiceInput {
		location: "local"
		interfaces: TwiceAPI
	}

	main
	{
		twice( x )( x * Factor )
	}
}