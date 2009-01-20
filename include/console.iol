inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in(string)
}

outputPort Console {
RequestResponse:
	print(undefined)(void), println(undefined)(void), registerForInput(void)(void)
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}
