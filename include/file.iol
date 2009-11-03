include "types/IOException.iol"

type ReadFileRequest:void {
	.filename:string
	.format?:string // Can be "base64", "binary", "text" or "xml" (defaults to "text")
}

type WriteFileRequest:void {
	.filename:string
	.content:undefined
	.format?:string // Can be "binary", "text" or "xml" (defaults to "text")
	.append?:int // Default: 0
}

type DeleteRequest:string { // The filename to delete
	.isRegex?:int // 1 if the filename is a regular expression, 0 otherwise
}

type RenameRequest:void {
	.filename:string
	.to:string
}

type ListRequest:void {
	.directory:string
	.regex:string
}

type ListResponse:void {
	.result[0,*]:string
}

outputPort File {
RequestResponse:
	readFile(ReadFileRequest)(undefined) throws FileNotFound(void) IOException(IOExceptionType),
	writeFile(WriteFileRequest)(void) throws FileNotFound(void) IOException(IOExceptionType),
	delete(DeleteRequest)(int) throws IOException(IOExceptionType),
	rename(RenameRequest)(void) throws IOException(IOExceptionType),
	list(ListRequest)(ListResponse),
	getServiceDirectory(void)(string),
	getFileSeparator(void)(string),
	getMimeType(string)(string) throws FileNotFound(void)
}

embedded {
Java:
	"joliex.io.FileService" in File
}
