inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in(string)
}

outputPort Console {
RequestResponse:
	print(string)(void), println(string)(void), registerForInput(void)(void)
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}
