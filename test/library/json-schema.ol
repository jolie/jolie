/*

 * Copyright (c) 2025 Claudio Guidi <guidiclaudio@gmail.com>

 *   This program is free software; you can redistribute it and/or modify 
 *   it under the terms of the GNU Library General Public License as      
 *   published by the Free Software Foundation; either version 2 of the   
 *   License, or (at your option) any later version.                      
 *                                                                        
 *   This program is distributed in the hope that it will be useful,      
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of       
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 *   GNU General Public License for more details.                         
 *                                                                        
 *   You should have received a copy of the GNU Library General Public    
 *   License along with this program; if not, write to the                
 *   Free Software Foundation, Inc.,                                      
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            
 *                                                                        
 *   For details about the authors of this software, see the AUTHORS file.
*/

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