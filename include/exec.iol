outputPort Exec {
RequestResponse:
	exec
}

embedded {
Java:
	"joliex.util.ExecService" in Exec
}