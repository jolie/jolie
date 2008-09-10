outputPort SMTP {
RequestResponse:
	sendMail throws SMTPFault
}

embedded {
Java:
	"joliex.mail.SMTPService" in SMTP
}

