outputPort XmlUtils {
RequestResponse:
	xmlToValue throws IOException,
	transform throws TransformerException
}

embedded {
Java:
	"joliex.util.XmlUtils" in XmlUtils
}
