outputPort StringUtils {
SolicitResponse:
	replaceAll, split, trim, contains
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}