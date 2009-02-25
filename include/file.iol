type DeleteRequest:string {
	.isRegex?:int
}

outputPort File {
RequestResponse:
	readFile throws FileNotFound IOException,
	writeFile throws FileNotFound IOException,
	delete(DeleteRequest)(int) throws IOException,
	rename throws IOException,
	list,
	getServiceDirectory,
	getFileSeparator
}

embedded {
Java:
	"joliex.io.FileService" in File
}