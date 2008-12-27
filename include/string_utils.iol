outputPort StringUtils {
RequestResponse:
	replaceAll, split, trim, contains, match, leftPad, join, splitByLength
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}