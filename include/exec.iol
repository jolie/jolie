type CommandExecutionRequest:string {
	.waitFor?:int
	.args[0,*]:string
}

type CommandExecutionResult:any { // Can be string or void
	.exitCode?:int
}


outputPort Exec {
RequestResponse:
	exec(CommandExecutionRequest)(CommandExecutionResult)
}

embedded {
Java:
	"joliex.util.ExecService" in Exec
}