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
	getServiceDirectory@File()( dir ) ;

	// INI file
	parseIniFile@IniUtils( dir + "/" + "private/odbc.ini" )( data );
	checkResult;

	// Plain
	readFile@File( { .filename = dir + "/../" + "../README.md", .format = "text" } )( data );
	checkResult;

	// Properties file
	readFile@File( { .filename = dir + "/../" + "../buildconfig/build.properties", .format = "properties" } )( data );
	checkResult;

	// XML file
	readFile@File( { .filename = dir + "/../" + "../build.xml", .format = "xml" } )( data );
	checkResult
}
