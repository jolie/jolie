outputPort Math {
RequestResponse:
	random(void)(double)
}

embedded {
Java:
	"joliex.util.MathService" in Math
}
