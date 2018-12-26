include "../AbstractTestUnit.iol"
include "file.iol"
include "ini_utils.iol"

define testList
{
	getServiceDirectory@File()( dir );
	getFileSeparator@File()( fs );

	listDir = dir + fs + "private" + fs + "file_list";
	list@File( {
		.directory = listDir,
		.order.byname = true
	} )( response );
	if ( #response.result != 2 ) {
		throw( TestFailed, "list@File: wrong number of results (expected 2, got " + #response.result + ")" )
	};
	if ( response.result[0] != listDir + fs + "README" ) {
		throw( TestFailed, "list@File: wrong result[0]. Expected " + listDir + fs + "README, got " + response.result[0] )
	};
	if ( response.result[1] != listDir + fs + "text1.txt" ) {
		throw( TestFailed, "list@File: wrong result[1]. Expected " + listDir + fs + "text1.txt, got " + response.result[1] )
	}
}

define checkResult
{
	if ( !is_defined( data ) ) {
		throw( TestFailed, "Could not read/parse file" )
	}
}

define doTest
{
	// INI file
	parseIniFile@IniUtils( "library/private/odbc.ini" )( data );
	checkResult;

	// Plain
	readFile@File( { .filename = "../README.md", .format = "text" } )( data );
	checkResult;

	// Properties file
	readFile@File( { .filename = "../buildconfig/build.properties", .format = "properties" } )( data );
	checkResult;

	// XML file
	readFile@File( { .filename = "../build.xml", .format = "xml" } )( data );
	checkResult;

	getServiceDirectory@File()( dir );
	getFileSeparator@File()( fs );
	setMimeTypeFile@File( dir + fs + "private" + fs + "mime.types" )();
	getMimeType@File( dir + fs + "private" + fs + "text.txt" )( mime );
	if ( mime != "text/plain" ) {
		throw( TestFailed, "Wrong mime type " + mime + " (expected text/plain)" )
	}

	testList
}
