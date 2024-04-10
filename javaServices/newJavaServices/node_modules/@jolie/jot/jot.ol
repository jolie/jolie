from file import File
from reflection import Reflection
from string_utils import StringUtils
from console import Console
from runtime import Runtime
from time import Time
from .jotutils import JotUtils
from .reporters.interfaces import ReporterInterface

/**
	Parameter from json's configuration
*/
type Params {
	test?: string // Path to test directory
	reporter? { // Reporter information
		path: string // Path to reporter directory
		service: string // Reporter's service name
	}
	params?: undefined // Parameter for target services
}

/**
	metrics data collected by the test runner
*/
type Stats {
	tests: int
	passes: int
	failures: int
	services: int
	durations: long
}

service Jot( params:Params ) {
	embed File as file
	embed JotUtils as jotUtils
	embed Reflection as reflection
	embed StringUtils as stringUtils
	embed Console as console
	embed Runtime as runtime
	embed Time as time

    outputPort Operation {
    }
	inputPort input {
		location: "local"
		RequestResponse: run
	}

	outputPort reporter {
		Interfaces: ReporterInterface
	}

	execution{ sequential }

	init {
		getRealServiceDirectory@file()( home )
		getFileSeparator@file()( sep )
		if (!is_defined(params.reporter)){
			params.reporter.path = home + sep + "reporters" + sep + "spec.ol"
			params.reporter.service = "SpecReporter"
		}
	}
	
	main {
		run()() {
			// load the reporter
			loadEmbeddedService@runtime( {
				filepath = params.reporter.path
				type = "Jolie"
				service = params.reporter.service
			} )( reporter.location )

			eventRunBegin@reporter()()

			getFileSeparator@file()( sep )
			
			scope (listFile) {
				install( NoSuchFileException => 
					println@console("Unable to locate test path looking for path: " + params.test)()
					exit
				)
				list@file( {
					directory = params.test
					regex = ".*\\.ol"
					recursive = true
				} )( foundFiles )
			}
			isFail = false
			for( filepath in foundFiles.result ) {

				findTestOperations@jotUtils( params.test + sep + filepath )( result )

				if (#result.services > 0){

					stats.tests = 0
					stats.passes = 0
					stats.failures = 0
					stats.services = 0


					stats.start = getCurrentTimeMillis@time()

					for( testServiceInfo in result.services ) {
						if( #testServiceInfo.tests > 0 ) {
							testParams << {}
							for (p in params.params){
								if ( is_defined(p.(filepath)) ) {
									if (p.(filepath).name == result.services.name) {
										testParams << p.(filepath).params
									}
								}
							}
							// load the testService in the outputPort testService
							loadEmbeddedService@runtime( {
								filepath = params.test + sep + filepath
								type = "Jolie"
								service = result.services.name
								params << testParams
							} )( Operation.location )

							stats.start = getCurrentTimeMillis@time()

							state.services++
							eventServiceBegin@reporter( { title= filepath + " -> " + result.services.name } )()

							for( op in testServiceInfo.beforeAll ) {
								scope ( bfAll ){
									install( default => 
										eventTestFail@reporter( { title = "[beforeAll] " + op error = bfAll.default } )()
										isFail = true
									)
									invokeRRUnsafe@reflection( { operation = op, outputPort="Operation" } )()
								}
							}
							for( test in testServiceInfo.tests ) {
								for( beforeEach in testServiceInfo.beforeEach ) {
									scope ( bfEach ){
										install( default => 
											eventTestFail@reporter( { title = "[beforeEach] " + test error = bfEach.default } )()
											isFail = true
										)
										invokeRRUnsafe@reflection( { operation = beforeEach, outputPort="Operation" } )()
									}
								}
								scope(t){
									install( default => 
										stats.failures++
										eventTestFail@reporter( { title = test error = t.InvocationFault.name + ": " + t.InvocationFault.data } )()
										isFail = true
									)
									invokeRRUnsafe@reflection( { operation = test, outputPort="Operation" } )()
									stats.tests++
									stats.passes++
									eventTestPass@reporter({ title = test })()
								}

								for( afterEach in testServiceInfo.afterEach ) {
									scope ( afEach ){
										install( default => 
											eventTestFail@reporter( { title = "[afterEach] " + afterEach error = afEach.default } )()
											isFail = true
										)
										invokeRRUnsafe@reflection( { operation = afterEach, outputPort="Operation" } )()
									}
								}
							}
							for( op in testServiceInfo.afterAll ) {
								scope ( afAll ){
									install( default => 
										eventTestFail@reporter( { title = "[afterAll] " + op error = afAll.default } )()
										isFail = true
									)
									invokeRRUnsafe@reflection( { operation = op, outputPort="Operation" } )()
								}
							}
							stats.durations = getCurrentTimeMillis@time() - stats.start
							eventServiceEnd@reporter({
								tests = stats.tests
								passes = stats.passes
								failures = stats.failures
								services = stats.services
								durations = stats.durations
							})()
						}
					}
				}
			}
			eventRunEnd@reporter()()
			if (isFail) {
				halt@runtime({status=1})()
			}
		}
	}
}