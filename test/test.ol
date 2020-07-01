/*
 * Copyright (C) 2009-2020 Fabrizio Monteis <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

include "TestUnit.iol"
include "console.iol"
include "file.iol"
include "runtime.iol"
include "string_utils.iol"

outputPort TestUnit {
interfaces: TestUnitInterface
}

init
{
	dirs[0] = "primitives"
	dirs[1] = "library"
	dirs[2] = "extensions"
	dirs[3] = "services"
}

define calcMaxLength
{
	maxLength = 0
	for( i = 0, i < #dirs, i++ ) {
		list@File( listRequest )( list )
		for( k = 0, k < #list.result, k++ ) {
			length@StringUtils( list.result[k] )( len )
			if ( len > maxLength ) {
				maxLength = len
			}
		}
	}
}

define printTestName
{
	testName += "..."
	length@StringUtils( testName )( len )
	for( j = 0, j < maxLength + 5 - len, j++ ) {
		testName += " "
	}
	print@Console( testName )()
}

main
{
	loadRequest.type = "Jolie"
	if ( is_defined( args[0] ) ) {
		listRequest.regex = args[0]
	} else {
		listRequest.regex = ".*\\.ol"
	}
	listRequest.directory -> dirs[i]
	calcMaxLength
	exitCode = 0 // Successful exit code
	for( i = 0, i < #dirs, i++ ) {
		list@File( listRequest )( list );
		for( k = 0, k < #list.result, k++ ) {
			testName = list.result[k]
			printTestName
			loadRequest.filepath = listRequest.directory + "/" + list.result[k]
			scope( s ) {
				install( RuntimeException => println@Console( s.RuntimeException.stackTrace )() )
				loadEmbeddedService@Runtime( loadRequest )( TestUnit.location )
				install(
					TestFailed => println@Console( "failed. " + s.TestFailed )(); exitCode = 3,
					Timeout => println@Console( "timed out." )(); exitCode = 3
				)
				test@TestUnit()()
				println@Console( "passed." )()
				callExit@Runtime( TestUnit.location )()
			}
		}
	}

	if( exitCode != 0 ) {
		halt@Runtime( { status = exitCode } )()
	}
}
