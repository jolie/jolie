type Message:void {
	.operationName:string
	.resourcePath:string
	.value:any { ? }
	.fault?:string {
		.value:any { ? }
	}
}

type MessageWithProtocol:void {
	.operationName:string
	.resourcePath:string
	.value:any { ? }
	.fault?:string {
		.value:any { ? }
	}
	
	.protocol:string { ? }
}

interface ProtocolAdapterInterface {
RequestResponse:
	send(Message)(MessageWithProtocol) throws IOException,
	recv(Message)(Message) throws IOException
}
