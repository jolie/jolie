include "../AbstractTestUnit.iol"
include "file.iol"
include "ini_utils.iol"

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
	checkResult
}
