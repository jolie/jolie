from string_utils import StringUtils
from console import Console

interface RenderInterface {
    RequestResponse: 
     typeRender(undefined)(undefined)
}

service MDRenderService {

    embed StringUtils as stringutils
    embed Console as console

    inputPort render {
        Interfaces: RenderInterface
        Location : "local"
        Protocol : sodep
    }

    outputPort selfRender {
        Interfaces: RenderInterface
        Location : "local"
        Protocol : sodep
        
    }

    main{
        

        [typeRender(request)(response){
            valueToPrettyString@stringutils( request )( s )
            println@console( s )(  )
            for ( counterFields  = 0 , counterFields < #request.fields , counterFields++){
                        response +=  "{\n"
                        response += "\t" +  request.fields[counterFields].name 

                        if (is_defined (request.fields[counterFields].type.fields)){
                               typeRender@selfRender(request.fields[counterFields].type.fields)(typeRenderResponse)
                               response += ":" + request.fields[counterFields].type.nativeType + typeRenderResponse
                        }else{
                              response += ":"  + request.fields.type.fields[counterFields].type.nativeType + "\n"
                        }
                        response +=  "}\n"
            }

        }]
    }
}