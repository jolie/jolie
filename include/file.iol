outputPort File {
RequestResponse:
	readFile throws FileNotFound IOException,
	writeFile throws FileNotFound IOException,
	list
}

embedded {
Java:
	"joliex.io.FileService" in File
}