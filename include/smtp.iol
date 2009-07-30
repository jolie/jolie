include "types/JavaException.iol"

type SendMailRequest:void {
	.host:string
	.authenticate?:int // 1 if host needs authentication, 0 otherwise.
	.username?:string
	.password?:string
	.from:string
	.to[1,*]:string
	.cc[0,*]:string
	.bcc[0,*]:string
	.subject:string
	.content:string
}

interface SMTPInterface {
RequestResponse:
	sendMail(SendMailRequest)(void) throws SMTPFault(JavaExceptionType)
}

outputPort SMTP {
Interfaces: SMTPInterface
}

embedded {
Java:
	"joliex.mail.SMTPService" in SMTP
}

