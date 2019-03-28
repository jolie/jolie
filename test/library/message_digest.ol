include "../AbstractTestUnit.iol"
include "message_digest.iol"
include "converter.iol"

define doTest
{
	str = "Hello, world!";
	expectedMD5 = "6cd3556deb0da54bca060b4c39479839";
	md5@MessageDigest( str )( result );
	if ( result != expectedMD5 ) {
		throw( TestFailed, "Wrong md5 for a string" )
	};

	stringToRaw@Converter( str { .charset = "UTF8" } )( buf );
	md5@MessageDigest( buf )( result );
	if ( result != expectedMD5 ) {
		throw( TestFailed, "Wrong md5 for a byte array" )
	}
}
