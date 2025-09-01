from string-utils import StringUtils
from console import Console


type ParseArgsRequest {
    args*: string 
    argsMap* {
        name: string        //< name of the expected argument
        optional: bool      //< specifies if the argument is optional
        description: string //< describes the argument
        isFlag: bool        //< if true, the argument is a flag (e.g. --verbose)
    }

}

type ParseArgsResponse {
    parsedArgs* {
        name: string 
        value: string | void
    }
}

interface ArgsInterface {
    RequestResponse:
        parseArgs( ParseArgsRequest )( ParseArgsResponse ) throws ParsingError( string ) Help

}

/*
 * This module allows for an easy parsing of the command line arguments
 */

service Args {

    execution: concurrent

    embed StringUtils as StringUtils
    embed Console as Console

    inputPort Args {
        location: "local"
        protocol: sodep 
        interfaces: ArgsInterface
    }

    define print_help {
        println@Console("")()
        println@Console("Documentation:\n")()
        for ( arg in request.argsMap ) {
            print@Console( "--" + rightPad@StringUtils( arg.name { length = 25, char = " " } ))()
            if ( arg.optional ) { print@Console( "[optional] " )() } 
            else { print@Console( "[required] " )() }
            if ( arg.isFlag ) { print@Console( "[flag argument] " )() } 
            else { print@Console( "[require value] " )() }
            println@Console( arg.description )()
        }
    }


    init {
        install( ParsingError => nullProcess )
        install( Help => nullProcess )
    }

    main {
        parseArgs( request )( response ) {
            // compose all the args into a single string, this allows for having strings as arguments
            args_string = ""
            for( a in request.args ) {
                args_string = args_string + a + " "
            }
            findAll@StringUtils( args_string { regex = "--\\w+(?:=.*?(?=\\s--|$))?" })( result )
            for( g in result.group ) {
                trim@StringUtils( g )( param )
                // separate from =
                split@StringUtils( param { regex = "=" } )( paramResult )
                // get the name
                split@StringUtils( paramResult.result[0] { regex = "--" } )( nameSplit )
                response.parsedArgs[ #response.parsedArgs ] << {
                    name = nameSplit.result[1]
                    value = paramResult.result[1]
                }       
                parsedArgsHashMap.( nameSplit.result[1] ) << true {
                    value = paramResult.result[1]
                }
            }

            // check if help has been requested
            if ( is_defined( parsedArgsHashMap.help ) ) {
                print_help
                throw Help
            } else {
                // check optional arguments
                for( arg in request.argsMap ) {
                    if ( !arg.optional && !is_defined( parsedArgsHashMap.( arg.name ) ) ) {
                        println@Console( "❌ Error: Missing required argument: " + arg.name )()
                        print_help
                        throw ParsingError( "Missing required argument: " + arg.name )
                    }
                    if ( is_defined( parsedArgsHashMap.( arg.name ) ) && arg.isFlag && !(parsedArgsHashMap.( arg.name ).value instanceof void ) ) {
                        println@Console( "❌ Error: Argument " + arg.name + " is a flag and cannot have a value\n" ) ()
                        print_help
                        throw ParsingError( "Argument " + arg.name + " is a flag and cannot have a value" )
                    }
                    if ( is_defined( parsedArgsHashMap.( arg.name ) ) && !arg.isFlag && (parsedArgsHashMap.( arg.name ).value instanceof void ) ) {
                        println@Console( "❌ Error: Argument " + arg.name + " must have a value.\n" )()
                        print_help
                        throw ParsingError( "Argument " + arg.name + " must have a value" )
                    }
                }
            }
        }
    }

}