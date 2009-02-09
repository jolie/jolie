inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in(string)
}

interface ConsoleInterface {
RequestResponse:
	print(undefined)(void), println(undefined)(void), registerForInput(void)(void)
}

outputPort Console {
Interfaces: ConsoleInterface
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}
