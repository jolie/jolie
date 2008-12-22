inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in
}

outputPort Console {
RequestResponse:
	print, println, registerForInput, in
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}
