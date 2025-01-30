from ..test-unit import TestUnitInterface
from file import File
from ini-utils import IniUtils 
from string-utils import StringUtils
from .private.services.service-directory import GetServiceDirectoryService 

service Main {

    embed File as File
	embed IniUtils as IniUtils
	embed StringUtils as StringUtils
	embed GetServiceDirectoryService as GetServiceDirectoryService

    inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

	define testList
	{
		getServiceDirectory@File()( dir )
		getFileSeparator@File()( fs )

		listDir = dir + fs + "private" + fs + "file_list"
		list@File( {
			directory = listDir
			order.byname = true
		} )( response )
		if ( #response.result != 3 ) {
			throw( TestFailed, "list@File: wrong number of results. Expected 3, got " + #response.result )
		}
		if ( response.result[0] != "README" ) {
			throw( TestFailed, "list@File: wrong result[0]. Expected " + listDir + fs + "README, got " + response.result[1] )
		}
		if ( response.result[1] != "subdir1" ) {
			throw( TestFailed, "list@File: wrong result[1]. Expected " + listDir + fs + "subdir1, got " + response.result[2] )
		}

		list@File( {
			directory = listDir
			info = true
		} )( response )

		for( f in response.result ) {
			if ( f.info.size == 0 && !f.info.isDirectory ) {
					throw( TestFailed, f + " should not have size 0 " )
			}
			if ( f.info.lastModified == 0 ) {
					throw( TestFailed, f + " has lastModified = 0 " )
			}
			if ( f == "subdir1" ) {
				if ( !f.info.isDirectory ) {
					throw( TestFailed, "subdir1 is a directory" )
				}
			} else {
				if ( f.info.isDirectory ) {
					throw( TestFailed, f + " is not a directory" )
				}
			}
		}
	}

	define checkResult
	{
		if ( !is_defined( data ) ) {
			throw( TestFailed, "Could not read/parse file" )
		}
	}

    main {
        test()() {
			dirLength = length@StringUtils( getServiceDirectory@File() )
			serDir =  getSerDir@GetServiceDirectoryService()
			serDirSuffix = substring@StringUtils( serDir {
				begin = dirLength 
			})
			if ( serDirSuffix != "/private/services" ) {
				throw( TestFailed, "getServiceDirectory from embedded service is wrong. Expected /private/services, found " + serDirSuffix )
			}

			// INI file
			parseIniFile@IniUtils( "library/private/odbc.ini" )( data )
			checkResult

			// Plain
			readFile@File( { .filename = "../README.md", .format = "text" } )( data )
			checkResult

			// // Properties file
			readFile@File( { .filename = "../libjolie/src/main/resources/libjolie.properties", .format = "properties" } )( data )
			checkResult

			// XML file
			readFile@File( { .filename = "../pom.xml", .format = "xml" } )( data )
			checkResult

			getServiceDirectory@File()( dir )
			getFileSeparator@File()( fs )
			setMimeTypeFile@File( dir + fs + "private" + fs + "mime.types" )()
			getMimeType@File( dir + fs + "private" + fs + "text.txt" )( mime )
			if ( mime != "text/plain" ) {
				throw( TestFailed, "Wrong mime type " + mime + " (expected text/plain)" )
			};

			testList

			scope( rename_scope ) {
				install( IOException => nullProcess )
				rename@File({ .filename = "aaa.a", .to = "bbb.b" } )()
			}
		}
	}
}
