outputPort StringUtils {
RequestResponse:
	replaceAll, split, trim, contains, match, leftPad
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}