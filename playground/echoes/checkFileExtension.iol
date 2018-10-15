define checkFileExtension
{
	s = file.filename;
	s.regex = "\\.";
	split@StringUtils( s )( ss );
	ext = ss.result[ #ss.result - 1 ];

	/** Images **/
	if (
		ext == "png" ||	ext == "gif"
		||
		ext == "jpg" || ext == "bmp"
		||
		ext == "xpm"
	) {
		type = "image/" + ext;
		file.format = "binary";
		c = request.("@UserAgent");
		c.regex = "(.*WebKit.*)|(.*MSIE.*)";
		match@StringUtils( c )( ret );
		if ( ret == 1 ) {
			format = "html";
			charset = "ISO-8859-1"
		} else {
			format = "binary"
		}
	} else if (
		ext == "css" || ext == "html"
	) {
		type = "text/" + ext
	}
}
