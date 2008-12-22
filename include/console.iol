inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in
}

outputPort Console {
RequestResponse:
	print, println, registerForInput, in
}

println@Console("Ciao");
a = 2;

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}