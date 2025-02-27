from ..test-unit import TestUnitInterface
from json-schema import JsonSchema
from metajolie import MetaJolie
from console import Console
from string-utils import StringUtils
from json-utils import JsonUtils
from file import File


service Main {

    embed JsonSchema as JsonSchema
    embed MetaJolie as MetaJolie
    embed Console as Console
    embed StringUtils as StringUtils
    embed JsonUtils as JsonUtils
    embed File as File

	inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {

            readFile@File( { filename ="library/private/openapi-schema2_0.json" })( openapi_schema2 )
            readFile@File( { filename ="library/private/openapi-schema3_0.json" })( openapi_schema3 )

            getInputPortMetaData@MetaJolie( { filename = "private/sample_service_joliedoclib.ol" } )( meta_description )
            for ( i in meta_description.input.interfaces ) {
                for( t in i.types ) {

                    // 2.0
                    scope( generation ) {
                        install( GenerationError => if ( t.name != "TChoice" && t.name != "TNative") {
                            throw( TestFailed, generation.GenerationError )
                        })
                        getTypeDefinition@JsonSchema( {
                            schemaVersion = "2.0" 
                            typeDefinition << t })( jsonschema )

                        getJsonString@JsonUtils( jsonschema )( schema_string )
                        validateJson@JsonUtils({
                            json = "{\n" 
                                + "  \"swagger\": \"2.0\",\n" 
                                + "  \"info\": { \"title\": \"Test API\", \"version\": \"1.0\" },\n" 
                                + "  \"paths\": {},\n" 
                                + "  \"definitions\":\n" 
                                + schema_string 
                                + "}"
                            schema = openapi_schema2 
                        })( validation )

                        if ( is_defined( validation.validationMessage ) ) {
                            valueToPrettyString@StringUtils( validation )( errors )
                            throw( TestFailed, "schema:" + schema_string + "\n" + errors )
                        }
                    }

                    // 3.0
                    getTypeDefinition@JsonSchema( {
                        schemaVersion = "3.0" 
                        typeDefinition << t })( jsonschema )
                    getJsonString@JsonUtils( jsonschema )( schema_string )


                    validateJson@JsonUtils({
                        json = "{\n" 
                            + "  \"openapi\": \"3.0.0\",\n" 
                            + "  \"info\": { \"title\": \"Test API\", \"version\": \"1.0\" },\n" 
                            + "  \"paths\": {},\n" 
                            + "  \"components\": {\n"
                            + "     \"schemas\":" 
                            + schema_string 
                            + "  }\n"
                            + "}"
                        schema = openapi_schema3 
                    })( validation )

                   

                    if ( is_defined( validation.validationMessage ) ) {
                        valueToPrettyString@StringUtils( validation )( errors )
                        throw( TestFailed, "schema:" + schema_string + "\n" + errors )
                    }
                }
            }
        }
    }

}