/*
 * Copyright (C) 2009-2021 Fabrizio Monteis <famontesi@gmail.com>
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

from .test-unit import TestUnitInterface
from console import Console
from file import File
from runtime import Runtime
from string-utils import StringUtils

service Main {
	outputPort TestUnit {
		interfaces: TestUnitInterface
	}

	embed Console as Console
	embed File as File
	embed Runtime as Runtime
	embed StringUtils as StringUtils

	init {
		dirs[0] = "primitives"
		dirs[1] = "library"
		dirs[2] = "extensions"
		dirs[3] = "services"
	}

	define calcMaxLength {
		maxLength = 0
		for( listRequest.directory in dirs ) {
			list@File( listRequest )( list )
			for( k = 0, k < #list.result, k++ ) {
				length@StringUtils( list.result[k] )( len )
				if ( len > maxLength ) {
					maxLength = len
				}
			}
		}
	}

	define printTestName {
		testHeader = testName + "..."
		length@StringUtils( testHeader )( len )
		for( j = 0, j < maxLength + 5 - len, j++ ) {
			testHeader += " "
		}
		print@Console( testHeader )()
	}

	main {
		if( is_defined( args[0] ) ) {
			listRequest.regex = args[0]
		} else {
			listRequest.regex = ".*\\.ol"
		}
		calcMaxLength
		exitCode = 0 // Successful exit code
		for( listRequest.directory in dirs ) {
			list@File( listRequest )( list )
			for( testName in list.result ) {
				printTestName
				scope( s ) {
					install( RuntimeException => println@Console( "failed (RuntimeException). " + s.RuntimeException.stackTrace )(); exitCode = 3 )
					loadEmbeddedService@Runtime( {
						type = "Jolie"
						service = "Main"
						filepath = listRequest.directory + "/" + testName
					} )( TestUnit.location )
					install(
						TestFailed => println@Console( "❌ failed (TestFailed). " + s.TestFailed )(); exitCode = 3,
						default => println@Console( "❌ failed (default handler)." )(); exitCode = 3,
						Timeout => println@Console( "🕑 timed out." )(); exitCode = 3
					)
					test@TestUnit()()
					println@Console( "passed ✅" )()
					callExit@Runtime( TestUnit.location )()
				}
			}
		}

		if( exitCode != 0 ) {
			halt@Runtime( { status = exitCode } )()
		}
	}
}
