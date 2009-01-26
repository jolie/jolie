outputPort Math {
RequestResponse:
	abs(int)(int), random(void)(double)
}

embedded {
Java:
	"joliex.util.MathService" in Math
}
