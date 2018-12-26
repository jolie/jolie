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
	if ( #response.result != 4 ) {
		throw( TestFailed, "list@File: wrong number of results. Expected 4, got " + #response.result )
	};
	if ( response.result[1] != "README" ) {
		throw( TestFailed, "list@File: wrong result[1]. Expected " + listDir + fs + "README, got " + response.result[1] )
	};
	if ( response.result[2] != "subdir1" ) {
		throw( TestFailed, "list@File: wrong result[2]. Expected " + listDir + fs + "subdir1, got " + response.result[2] )
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
	};

	testList
}
