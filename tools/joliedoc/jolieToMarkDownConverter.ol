from inspector import Inspector
from string_utils import StringUtils
from console import Console
from file import File

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
    
    

    inputPort ConverterPort {
        Interfaces: DocumentConverterInterface , RenderInterface
        Location : "local"
    }

    outputPort render {
        Interfaces: RenderInterface
    }


   execution{ concurrent }


    main{


        [typeRender(request)(response){

            for ( counterFields  = 0 , counterFields < #request.fields , counterFields++){
                        response +=  "{\n"
                        response += "\t" +  request.fields[counterFields].name 
                        if (is_defined (request.fields[counterFields].fields)){
                               typeRender@render(request.fields[counterFields])(typeRenderResponse)
                               typeJolie += ":" + data.result.referredTypes[counterTypes].type.fields[counterFields].type.nativeType + typeRender
                        }else{
                               typeJolie += ":"  + data.result.referredTypes[counterTypes].type.fields[counterFields].type.nativeType + "\n"
                        }
                        response +=  "}\n"
            }

        }]

          [convertDocument(request)(reponse){
                inspectPorts@inspector( {filename = request.filename} )( data.result ) 
                valueToPrettyString@stringutils(data)(s)
                println@console(s)()

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