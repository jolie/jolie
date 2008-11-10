outputPort XmlUtils {
RequestResponse:
	xmlToValue throws IOException
}

embedded {
Java:
	"joliex.util.XmlUtils" in XmlUtils
}
