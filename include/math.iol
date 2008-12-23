outputPort Math {
RequestResponse:
	random
}

embedded {
Java:
	"joliex.util.MathService" in Math
}
