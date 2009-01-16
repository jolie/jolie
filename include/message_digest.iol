type MD5Request:string {
	.radix?:int
}


outputPort MessageDigest {
RequestResponse:
	md5(MD5Request)(string) throws UnsupportedOperation
}

embedded {
Java:
	"joliex.security.MessageDigestService" in MessageDigest
}
