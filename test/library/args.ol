from ..test-unit import TestUnitInterface
from args import Args 


service Main {

    embed Args as Args

    inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {

            getParsedArgs@Args({
                args[ 0 ] = "--arg1=value1"
                args[ 1 ] = "--arg2=value2"
                args[ 2 ] = "--arg3=value3 ja ja "
                argsMap[ 0 ] << { name = "arg1", optional = false }
                argsMap[ 1 ] << { name = "arg2", optional = false }
                argsMap[ 2 ] << { name = "arg3", optional = false }
                argsMap[ 3 ] << { name = "arg4", optional = true }

            })( result )

            if ( #result.parsedArgs != 3 ) {
                throw TestFailed( "Expected 3 parsed arguments, got " + #result.parsedArgs )
            }
            if ( result.parsedArgs[0].name != "arg1" || result.parsedArgs[0].value != "value1" ) {
                throw TestFailed( "First argument is not parsed correctly: " + result.parsedArgs[0].name + "=" + result.parsedArgs[0].value )
            }
            if ( result.parsedArgs[1].name != "arg2" || result.parsedArgs[1].value != "value2" ) {
                throw TestFailed( "Second argument is not parsed correctly: " + result.parsedArgs[1].name + "=" + result.parsedArgs[1].value )
            }
            if ( result.parsedArgs[2].name != "arg3" || result.parsedArgs[2].value != "value3 ja ja" ) {
                throw TestFailed( "Third argument is not parsed correctly: " + result.parsedArgs[2].name + "=" + result.parsedArgs[2].value )
            }  

            // Test for missing required argumen
            scope( check_error ) {
                install( ParametersError => 
                    nullProcess
                )
                getParsedArgs@Args({
                    args[ 0 ] = "--arg1=value1"
                    args[ 1 ] = "--arg2=value2"
                    args[ 2 ] = "--arg3=value3 ja ja "
                    argsMap[ 0 ] << { name = "arg1", optional = false }
                    argsMap[ 1 ] << { name = "arg2", optional = false }
                    argsMap[ 2 ] << { name = "arg3", optional = false }
                    argsMap[ 3 ] << { name = "arg4", optional = false }

                })( result )

                throw TestFailed( "Expected ParametersError for missing required argument, but got no error" ) 
            }
        }
    }
}