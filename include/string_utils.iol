outputPort StringUtils {
RequestResponse:
	replaceAll, split, trim, contains, match, leftPad, join
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}