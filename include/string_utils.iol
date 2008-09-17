outputPort StringUtils {
RequestResponse:
	replaceAll, split, trim, contains, match
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}