outputPort File {
RequestResponse:
	readFile throws FileNotFound IOException,
	writeFile throws FileNotFound IOException,
	delete throws IOException,
	rename throws IOException,
	list
}

embedded {
Java:
	"joliex.io.FileService" in File
}