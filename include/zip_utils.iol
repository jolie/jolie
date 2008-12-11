outputPort ZipUtils {
RequestResponse:
	readEntry throws IOException,
	zip throws IOException
}

embedded {
Java:
	"joliex.util.ZipUtils" in ZipUtils
}
