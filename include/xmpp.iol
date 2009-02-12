type ConnectionRequest:void {
	.serviceName:string
	.host?:string
	.port?:int
	.username:string
	.password:string
	.resource?:string
}

type SendMessageRequest:string {
	.to:string
}

interface XMPPInterface {
RequestResponse:
	connect(ConnectionRequest)(void) throws XMPPException,
	sendMessage(SendMessageRequest)(void) throws XMPPException
}

outputPort XMPP {
Interfaces: XMPPInterface
}

embedded {
Java:
	"joliex.xmpp.XMPPService" in XMPP
}
