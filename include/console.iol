inputPort ConsoleInputPort {
OneWay:
	in
}

outputPort Console {
Notification:
	print, println
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}