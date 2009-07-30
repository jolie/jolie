type CommandExecutionRequest:string { // The command to execute
	.waitFor?:int // 1 if the command is to be waited for, 0 otherwise
	.args[0,*]:string // Arguments to be passed to the command
}

type CommandExecutionResult:any { // Can be string or void
	.exitCode?:int // The exit code of the executed command
	.stderr?:string // The standard error output of the executed command
}


outputPort Exec {
RequestResponse:
	exec(CommandExecutionRequest)(CommandExecutionResult)
}

embedded {
Java:
	"joliex.util.ExecService" in Exec
}
