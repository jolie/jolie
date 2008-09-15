outputPort File {
RequestResponse:
	readFile, list
}

embedded {
Java:
	"joliex.io.FileService" in File
}