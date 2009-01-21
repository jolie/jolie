type CommandExecutionRequest:string {
	.waitFor?:int
	.args[0,*]:string
}

type CommandExecutionResult:any { // Can be string or void
	.exitCode?:int
	.stderr?:string
}


outputPort Exec {
RequestResponse:
	exec(CommandExecutionRequest)(CommandExecutionResult)
}

embedded {
Java:
	"joliex.util.ExecService" in Exec
}
