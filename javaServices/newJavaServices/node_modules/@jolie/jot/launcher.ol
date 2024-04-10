#!/usr/bin/env -S jolie

/*
   Copyright 2020-2021 Fabrizio Montesi <famontesi@gmail.com>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

from runtime import Runtime
from file import File
from console import Console

service Launcher {

	outputPort wrapper {
		RequestResponse: run
	}

	embed Runtime as runtime
	embed File as file
	embed Console as console

	main {
		/*
		if( !is_defined( args[0] ) ) {
			println@console("Usage: jot configuration_file.json")()
			exit
		}
		*/
		config <<
			if( is_defined( args[0] ) )
				readFile@file( { filename = args[0], format = "json" } )
			else if( exists@file( "jot.json" ) )
				readFile@file( { filename = "jot.json", format = "json" } )
			else
				{}

		getRealServiceDirectory@file()( home )
		getFileSeparator@file()( sep )

		if( !is_defined(config.reporter) ) {
			config.reporter.path = home + sep + "reporters" + sep + "spec.ol"
			config.reporter.service = "SpecReporter"
		}

		if ( !is_defined(config.test) ){
			config.test = "test"
		}

		loadLibrary@runtime( home + sep + "lib" + sep + "jot-utils.jar" )()
		loadEmbeddedService@runtime( {
			filepath = home + sep + "jot.ol"
			service = "Jot"
			params << config
		} )(wrapper.location)

		scope( runner ){
			install( this => nullProcess )
			run@wrapper()()
		}
	}
}
