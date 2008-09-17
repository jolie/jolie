outputPort MessageDigest {
RequestResponse:
	md5 throws UnsupportedOperation
}

embedded {
Java:
	"joliex.security.MessageDigest" in MessageDigest
}