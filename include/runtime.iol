include "types/JavaException.iol"
include "types/IOException.iol"
include "types/Binding.iol"

type LoadEmbeddedServiceRequest:void {
	.filepath:string
	.type:string
}

type GetRedirectionRequest:void {
	.inputPortName:string
	.resourceName:string
}

type SetRedirectionRequest:void {
	.inputPortName:string
	.resourceName:string
	.outputPortName:string
}

type RuntimeExceptionType:JavaExceptionType

type SetOutputPortRequest:void {
	.name:string
	.location:any
	.protocol?:string { ? }
}

type SendMessageRequest:void {
	.operation:string
	.binding:Binding
	.message:undefined
}

interface RuntimeInterface {
RequestResponse:
	getLocalLocation(void)(any),
	loadEmbeddedService(LoadEmbeddedServiceRequest)(any) throws RuntimeException(RuntimeExceptionType),

	getRedirection(GetRedirectionRequest)(any),
	setRedirection(SetRedirectionRequest)(void) throws RuntimeException(RuntimeExceptionType),
	removeRedirection(GetRedirectionRequest)(void) throws RuntimeException(RuntimeExceptionType),

	setOutputPort(SetOutputPortRequest)(void),
	removeOutputPort(string)(void),
	callExit(any)(void)
}

outputPort Runtime {
Interfaces: RuntimeInterface
}

embedded {
Java:
	"joliex.lang.RuntimeService" in Runtime
}