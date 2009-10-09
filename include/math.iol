type RoundRequestType:double {
	.decimals?:int
}

outputPort Math {
RequestResponse:
	abs(int)(int),
	random(void)(double),
	round(RoundRequestType)(double)
}

embedded {
Java:
	"joliex.util.MathService" in Math
}
