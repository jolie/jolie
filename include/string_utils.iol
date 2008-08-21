outputPort StringUtils {
RequestResponse:
	replaceAll, split, trim, contains
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}