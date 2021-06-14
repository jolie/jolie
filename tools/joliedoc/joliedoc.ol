include "metajolie.iol"
include "console.iol"
include "types/definition_types.iol"
include "file.iol"
include "string_utils.iol"
include "runtime.iol"


type ConvertDocumentRequest:void{
    .filename: string
    .args: undefined
}


interface DocumentConverterInterface {
    RequestResponse:
      convertDocument(ConvertDocumentRequest)(void)
}


outputPort ConverterPort {
    Interfaces: DocumentConverterInterface
    Location : "local"
}



main {
     

    if ( #args == 0 || #args > 4 ) {
        println@Console( "Usage: joliedoc <filename> [--internals ] [--format [ html | md ]]")()
        halt@Runtime ({status = 1})()
    }else{

        if (("--help" == args[0]) || ("--h" == args[0])){
           println@Console( "Usage: joliedoc <filename> [--internals ] [--format [ html | md ]]")()
           halt@Runtime ({status = 0})()
        }
         
        parsedArguments.internal = false
        parsedArguments.format = "HTML"
        for ( counter = 1 , counter < #args , counter++ ){
            if ("--internals" == args[counter]){
              parsedArguments.internal = true
            }

            if ("--format" == args[counter]){
              toUpperCase@StringUtils( args[counter +1 ] )( parsedArguments.format )
            }
        }

        if ("HTML" == parsedArguments.format){
          with( emb ) {
            .filepath = __op + ".ol";
            .type = "Jolie"
           }
           loadEmbeddedService@Runtime( { filepath = "jolieHtmlConverter.ol", type = "Jolie"} )( ConverterPort.location )
            convertDocument@ConverterPort({filename = args[0]  args << parsedArguments})()
        }else if ("MD" == parsedArguments.format){
            loadEmbeddedService@Runtime( { filepath = "jolieToMarkDownConverter.ol", type = "Jolie"} )( ConverterPort.location )
            convertDocument@ConverterPort({filename = args[0]  args << parsedArguments})()
        }else{

            println@Console( parsedArguments.format  + " is not a supported format"  )(  )
            println@Console( "Usage: joliedoc <filename> [--internals ] [--format [ html | md ]]")()
            halt@Runtime ({status = 1})()
        }

    } 


}