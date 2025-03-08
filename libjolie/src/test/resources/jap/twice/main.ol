from .d1.d2.twice_api import TwiceAPI

constants {
	Factor = 2
}


service test {

	execution: concurrent



	inputPort TwiceInput {
		protocol: "http"
        location: "socket://localhost:17080"
		interfaces: TwiceAPI
	}

	main
	{
		twice( x )( x * Factor )
	}
}
