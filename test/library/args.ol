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

            parseArgs@Args({
                args[ 0 ] = "--arg1=value1"
                args[ 1 ] = "--arg2=value2"
                args[ 2 ] = "--arg3=value3 ja ja "
                argsMap[ 0 ] << { 
                    name = "arg1"
                    optional = false
                    description = "This is the first argument"
                    isFlag = false
                }
                argsMap[ 1 ] << { 
                    name = "arg2"
                    optional = false
                    description = "This is the second argument"
                    isFlag = false
                }
                argsMap[ 2 ] << { 
                    name = "arg3"
                    optional = false
                    description = "This is the third argument"
                    isFlag = false
                }
                argsMap[ 3 ] << { 
                    name = "arg4", 
                    optional = true,
                    description = "This is an optional argument",
                    isFlag = false
                }

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
                install( ParsingError =>
                    nullProcess
                )
                parseArgs@Args({
                    args[ 0 ] = "--arg1=value1"
                    args[ 1 ] = "--arg2=value2"
                    args[ 2 ] = "--arg3=value3 ja ja "
                    argsMap[ 0 ] << { 
                        name = "arg1"
                        optional = false
                        description = "This is the first argument"
                        isFlag = false
                    }
                    argsMap[ 1 ] << { 
                        name = "arg2"
                        optional = false
                        description = "This is the second argument"
                        isFlag = false
                    }
                    argsMap[ 2 ] << { 
                        name = "arg3"
                        optional = false
                        description = "This is the third argument"
                        isFlag = false
                    }
                    argsMap[ 3 ] << { 
                        name = "arg4", 
                        optional = false,
                        description = "This is an optional argument",
                        isFlag = false
                    }

                })( result )

                throw TestFailed( "Expected ParsingError for missing required argument, but got no error" ) 
            }

            // Test for missing required argumen
            scope( check_error ) {
                install( ParsingError => 
                     throw TestFailed( "No fault expected" ) 
                )
                parseArgs@Args({
                    args[ 0 ] = "--arg1=value1"
                    args[ 1 ] = "--arg2"
                    args[ 2 ] = "--arg3=value3 ja ja "
                    args[ 3 ] = "--arg4"
                    argsMap[ 0 ] << { 
                        name = "arg1"
                        optional = false
                        description = "This is the first argument"
                        isFlag = false
                    }
                    argsMap[ 1 ] << { 
                        name = "arg2"
                        optional = false
                        description = "This is the second argument"
                        isFlag = true
                    }
                    argsMap[ 2 ] << { 
                        name = "arg3"
                        optional = false
                        description = "This is the third argument"
                        isFlag = false
                    }
                    argsMap[ 3 ] << { 
                        name = "arg4", 
                        optional = true,
                        description = "This is an optional argument",
                        isFlag = true
                    }

                })( result )

               
            }

            // Test for missing required argumen
            scope( check_error ) {
                install( ParsingError => 
                     nullProcess
                )
                parseArgs@Args({
                    args[ 0 ] = "--arg1=value1"
                    args[ 1 ] = "--arg2"
                    args[ 2 ] = "--arg3=value3 ja ja "
                    args[ 3 ] = "--arg4=prova"
                    argsMap[ 0 ] << { 
                        name = "arg1"
                        optional = false
                        description = "This is the first argument"
                        isFlag = false
                    }
                    argsMap[ 1 ] << { 
                        name = "arg2"
                        optional = false
                        description = "This is the second argument"
                        isFlag = true
                    }
                    argsMap[ 2 ] << { 
                        name = "arg3"
                        optional = false
                        description = "This is the third argument"
                        isFlag = false
                    }
                    argsMap[ 3 ] << { 
                        name = "arg4", 
                        optional = true,
                        description = "This is an optional argument",
                        isFlag = true
                    }

                })( result )
                throw TestFailed( "Fault expected for arg4 where isFlag = true" ) 
            }
        }
    }
}