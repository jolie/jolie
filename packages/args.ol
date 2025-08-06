from string-utils import StringUtils


type GetParsedArgsRequest {
    args*: string 
    argsMap*: void {
        name: string        // name of the expected argument
        optional: bool      // specifies if the argument is optional
    }

}

type GetParsedResponse {
    parsedArgs* {
        name: string 
        value: string 
    }
}

interface ArgsInterface {
    RequestResponse:
        getParsedArgs( GetParsedArgsRequest )( GetParsedResponse ) throws ParametersError( string )

}

/*
 * This module allows for an easy parsing of the command line arguments
 */

service Args {

    execution: concurrent

    embed StringUtils as StringUtils

    inputPort Args {
        location: "local"
        protocol: sodep 
        interfaces: ArgsInterface
    }

    init {
        install( ParametersError => nullProcess )
    }

    main {
        getParsedArgs( request )( response ) {
            // compose all the args into a single string, this allows for having strings as arguments
            args_string = ""
            for( a in request.args ) {
                args_string = args_string + a + " "
            }
            findAll@StringUtils( args_string { regex = "--\\w+=.*?(?=\\s--|$)" })( result )
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
                parsedArgsHashMap.( nameSplit.result[1] ) = true
            }

            // check optional arguments
            for( arg in request.argsMap ) {
                if ( !arg.optional && !is_defined( parsedArgsHashMap.( arg.name ) ) ) {
                    throw ParametersError( "Missing required argument: " + arg.name )
                }
            }
        }
    }

}