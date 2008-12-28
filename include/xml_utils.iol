outputPort XmlUtils {
RequestResponse:
	xmlToValue throws IOException,
	transform throws TransformerException,
	valueToXml
}

embedded {
Java:
	"joliex.util.XmlUtils" in XmlUtils
}
