inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in(string)
}

outputPort Console {
RequestResponse:
	print(any)(void), println(any)(void), registerForInput(void)(void)
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}
