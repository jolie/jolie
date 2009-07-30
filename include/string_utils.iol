type ReplaceAllRequest:string {
	.regex:string
	.replacement:string
}

type JoinRequest:void {
	.piece[0,*]:string
	.delimiter:string
}

type SplitByLengthRequest:string {
	.length:int
}

type SplitResult:void {
	.result[0,*]:string
}

type SplitRequest:string {
	.limit?:int
	.regex:string
}

type PadRequest:string {
	.length:int
	.char:string
}

type MatchRequest:string {
	.regex:string
}

type MatchResult:int { // 1 if at least a match was found, 0 otherwise.
	.group[0,*]:string
}

type StartsWithRequest:string {
	.prefix:string
}

interface StringUtilsInterface {
RequestResponse:
	join(JoinRequest)(string),
	leftPad(PadRequest)(string),
	length(string)(int),
	match(MatchRequest)(MatchResult),
	replaceAll(ReplaceAllRequest)(string), 
	split(SplitRequest)(SplitResult),
	splitByLength(SplitByLengthRequest)(SplitResult),
	trim(string)(string),
	startsWith(StartsWithRequest)(int)
}

outputPort StringUtils {
Interfaces: StringUtilsInterface
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}
