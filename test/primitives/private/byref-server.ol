service ByRefServer {
	execution: sequential
	
	inputPort Input {
		location: "local"
		RequestResponse: run
	}

	main {
		run( request )() {
			request.x = 3
		}
	}
}