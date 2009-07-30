include "types/JavaException.iol"
include "types/IOException.iol"

// TODO: fault typing in the Java code

type XMLTransformationRequest:void {
	.source:string
	.xslt:string
}

outputPort XmlUtils {
RequestResponse:
	xmlToValue(any)(undefined) throws IOException(IOExceptionType),
	transform(XMLTransformationRequest)(string) throws TransformerException(JavaException),
	valueToXml(undefined)(string)
}

embedded {
Java:
	"joliex.util.XmlUtils" in XmlUtils
}
