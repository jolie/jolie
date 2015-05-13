include "types/Binding.iol"

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

/*
type Range:void { .min:int .max:int } // Both extremes are included

type NativeType
	: string("void")
	| string("int")
	| string("string")
	| string("double")
	| string("long")
	| string("raw")

type Type:void {
	.name:string
	.nativeType:NativeType
	.range:Range
	.subTypes*:Type
}

type OneWayOperation:void {
	.name:string
	.requestType:Type
}

type FaultType:void {
	.name:string
	.type:Type
}

type RequestResponseOperation:void {
	.name:string
	.requestType:Type
	.responseType:Type
	.faultType*:FaultType
}

type Operation:OneWayOperation | RequestResponseOperation

type Interface:void {
	.name:string
	.operation*:Operation
}
*/

type ReflectionSetOutputPortRequest:void {
	.name:string
	.binding:Binding
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