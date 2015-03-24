type InvokeRequest:void {
	.operation:string
	.outputPort:string
	.resourcePath?:string
	.data?:undefined
}

type InvocationFaultType:void {
	.name:string
	.data:string
}

/**!
WARNING: the API of this service is experimental. Use it at your own risk.
*/
interface ReflectionIface {
RequestResponse:
	/**!
	Invokes the specified .operation at .outputPort.
	If the operation is a OneWay, the invocation returns no value.
	*/
	invoke(InvokeRequest)(undefined) throws OperationNotFound(string) InvocationFault(InvocationFaultType)
}

outputPort Reflection {
Interfaces: ReflectionIface
}

embedded {
Java:
	"joliex.lang.reflection.Reflection" in Reflection
}