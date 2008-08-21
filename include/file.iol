outputPort File {
RequestResponse:
	readFile
}

embedded {
Java:
	"joliex.io.FileService" in File
}