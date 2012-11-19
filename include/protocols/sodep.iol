type SodepConfiguration:void {
	/*
	 * Defines the character set to use for (de-)coding strings.
	 *
	 * Default: "UTF-8"
	 * Supported values: "US-ASCII", "ISO-8859-1", "UTF-8", ... (all possible Java charsets)
	 */
	.charset?:string

	/*
	 * Compression algorithm to use.
	 *
	 * Default: void (no compression algorithm is used)
	 * Supported values: "gzip"
	 */
	.compression?:string

	/*
	 * Defines whether the underlying connection should be kept open.
	 *
	 * Default: true
	 */
	.keepAlive:bool
}
