outputPort MessageDigest {
RequestResponse:
	md5 throws UnsupportedOperation
}

embedded {
Java:
	"joliex.security.MessageDigestService" in MessageDigest
}
