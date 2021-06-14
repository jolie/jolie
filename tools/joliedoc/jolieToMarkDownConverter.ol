from inspector import Inspector
from string_utils import StringUtils
from console import Console
from file import File
from .markdownRenderService import MDRenderService, RenderInterface

type ConvertDocumentRequest:void{
    .filename: string
    .args: undefined
}


interface DocumentConverterInterface {
    RequestResponse:
      convertDocument(ConvertDocumentRequest)(void)
}

interface RenderInterface {
    RequestResponse: 
     typeRender(undefined)(undefined)
}


service JolieToMarkDownConverter {
    
    embed Inspector as inspector 
    embed StringUtils as stringutils
    embed Console as console
    embed File as file
    embed MDRenderService as render
    
    inputPort ConverterPort {
        Interfaces: DocumentConverterInterface
        Location : "local"
    }

   execution{ concurrent }

    main{

          [convertDocument(request)(reponse){
                inspectPorts@inspector( {filename = request.filename} )( data.result ) 
                valueToPrettyString@stringutils(data)(s)
                println@console(s)()
                fileContentMarkDown = ""
                for (counter = 0 , counter < #data.result.inputPorts  , counter++){
                        
                        for (counterInterfaces = 0 , counterInterfaces < #data.result.inputPorts[0].interfaces , counterInterfaces++){
                            fileContentMarkDown +=   "# " + data.result.inputPorts[counter].interfaces.name + "\n\n"
                            
                            fileContentMarkDown +=   data.result.inputPorts[counter].interfaces.documentation + "\n"
                            
                            fileContentMarkDown += "| Name of the Operation | RequestType | ResponseType | Documentation |\n"
                            fileContentMarkDown += "|---|---|---|---|\n"
                            for ( counterOperation  = 0 , counterOperation < #data.result.inputPorts[counter].interfaces[counterInterfaces].operations , counterOperation++){
                                fileContentMarkDown += "|" 
                                                        + data.result.inputPorts[counter].interfaces[counterInterfaces].operations[counterOperation].name 
                                                        + "|"
                                                        + data.result.inputPorts[counter].interfaces[counterInterfaces].operations[counterOperation].requestType   
                                                        + "|" 
                                                        + data.result.inputPorts[counter].interfaces[counterInterfaces].operations[counterOperation].responseType
                                                        + "|"
                                                        + data.result.inputPorts[counter].interfaces[counterInterfaces].operations[counterOperation].documentation
                                                        + "|\n"
                                data.typesFromOperation[#data.typesFromOperation] = data.result.inputPorts[counter].interfaces[counterInterfaces].operations[counterOperation].requestType
                                data.typesFromOperation[#data.typesFromOperation] = data.result.inputPorts[counter].interfaces[counterInterfaces].operations[counterOperation].responseType
                            }      

                        }  

                            
                    }

                    counter = 0 

                valueToPrettyString@stringutils(data)(s)
                println@console(s)()

                    while (counter < #data.typesFromOperation ){

                        for ( counterTypes  = 0 , counterTypes < #data.result.referredTypes , counterTypes++){
                        if (data.result.referredTypes[counterTypes].name == data.typesFromOperation[counter] ){
                            undef(typeJolie)
                            typeJolie += "type " +  data.result.referredTypes[counterTypes].name 
                                                    + ": " + data.result.referredTypes[counterTypes].type.nativeType 
                                if (#data.result.referredTypes[counterTypes].type.fields>0){
                                typeRender@render (data.result.referredTypes[counterTypes].type) (typeRenderResponse)
                                typeJolie+=typeRenderResponse
                                }                    
                            println@console(typeJolie)()
                        }   
                        }
                    counter++   
                    }

                    writeFile@file({filename="readme.md"
                                content =fileContentMarkDown })()
          

        }]
    }

}