inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in
}

outputPort Console {
OneWay:
	print, println, registerForInput
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}