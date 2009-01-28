type ReplaceAllRequest:string {
	.regex:string
	.replacement:string
}

interface StringUtilsInterface {
RequestResponse:
	join,
	leftPad,
	length(string)(int),
	match,
	replaceAll(ReplaceAllRequest)(string), 
	split,
	splitByLength,
	trim
}

outputPort StringUtils {
Interfaces: StringUtilsInterface
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}
