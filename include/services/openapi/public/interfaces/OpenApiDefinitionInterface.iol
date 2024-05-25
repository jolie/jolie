/*
 *   Copyright (C) 2016 by Claudio Guidi <guidiclaudio@gmail.com>         
 *                                                                        
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

include "types/definition_types.iol"

type ExternalDocs {
    url: string
    description?: string
}

type Item {
    type: string
    format?: string
    items*: Item
    maximum: int
    minimum: int
    /* NOT USED FOR Jolie purpouses
    .collectionFormat
    .default
    .exclusiceMaximum
    .exclusiceMinimum
    .maxLength
    .minLength
    .pattern
    .maxItems
    .minItems
    .uniqueItems
    .enum
    .multipleOf
  */
}

type OperationObject {
    tags*: string
    summary?: string
    description?: string
    externalDocs?: ExternalDocs
    operationId: string
    consumes*: string
    produces*: string
    parameters*: Parameter
    responses*: Responses
}

type InBody  {
    schema_type: Type         // used when there are more parameters in the body
} | void {
    schema_ref?: string       // add a reference to a schema
}

type Parameter {
    name: string
    in: void {
        in_body?:InBody
        other?: string {               // "query", "header", "path", "formData"
            type: Type
            allowEmptyValue?: bool
        }
    }
    required?: bool
    description?: string
}

type Responses {
    status: int 
    schema?: Type
    description: string
}

type GetOpenApiDefinitionRequest {
    info: void {
        title: string
        description?: string
        termsOfService?: string
        contact?: void {
            name?: string
            url?: string
            email?: string
        }
        license?: void {
            name: string
            url: string
        }
        version: string
    }
    host: string
    basePath: string
    schemes*: string
    consumes*: string
    produces*: string
    tags*: void {
        name: string
        description?: string
        externalDocs?: ExternalDocs
    }
    externalDocs?: ExternalDocs
    paths*: string {
        get?: OperationObject
        post?: OperationObject
        delete?: OperationObject
        put?: OperationObject
        /* TODO
        .options?: OperationObject
        .head?: OperationObject
        .patch?: OperationObject
        .parameters?:
        */
    }
    definitions*: TypeDefinition | FaultDefinitionForOpenAPI
    /* TODO
      .security?
      .securityDefinitions?
      .defintions?

    */
}

type FaultDefinitionForOpenAPI {
    fault: TypeDefinition
    name: string
}

type DefinitionIsArrayRequest {
    name: string
    definition: undefined
}

type GetOpenApiDefinitionResponse: undefined

type GetJolieTransformationFromSchemaRequest: void {
    schema: undefined 
    indentation: int
    array_def_list?: undefined
}

type GetJolieTransformationFromSchemaResponse: string {
    cardinality?: string
}

type GetJolieTypeFromOpenApiParametersRequest {
    definition: undefined
    name: string
    array_def_list?: undefined
}

type GetJolieTypeFromOpenApiDefinitionRequest {
    name: string
    definition: undefined
    array_def_list?: undefined
}

type GetJolieDefinitionFromOpenApiObjectRequest {
    definition: undefined
    indentation: int
    array_def_list?: undefined
}

type GetJolieDefinitionFromOpenApiArrayRequest {
    definition: undefined
    indentation: int
    array_def_list?: undefined
}

type GetJolieDefinitionFromOpenApiArrayResponse: string {
    cardinality: string 
}

type GetJolieNativeTypeFromOpenApiNativeTypeRequest {
    type?: string
    pattern?: string
    minLength?: int
    maxLength?: int
    enum[0,*]: string | void
    format?: string
    minimum?: int | long | double
    maximum?: int | long | double
    example?: string
    name?: string // FIXME: silently ignore unneeded attributes
    description?: string
    in?: string
    required?: bool
    default?: undefined
    allOf[0,*]: undefined
}

interface OpenApiDefinitionInterface {
  RequestResponse:
      setDefinitionsPath( string )( void ),  
      definitionIsArray( DefinitionIsArrayRequest )( bool ),
      getOpenApiDefinition( GetOpenApiDefinitionRequest )( GetOpenApiDefinitionResponse ),
      getJolieTransformationFromSchema( GetJolieTransformationFromSchemaRequest )( GetJolieTransformationFromSchemaResponse ) throws DefinitionError,
      getJolieTypeFromOpenApiDefinition( GetJolieTypeFromOpenApiDefinitionRequest )( string ),
      getJolieTypeFromOpenApiParameters( GetJolieTypeFromOpenApiParametersRequest )( string ),
      getJolieDefinitionFromOpenApiObject( GetJolieDefinitionFromOpenApiObjectRequest )( string )
        throws DefinitionError,
      getJolieDefinitionFromOpenApiArray( GetJolieDefinitionFromOpenApiArrayRequest )( GetJolieDefinitionFromOpenApiArrayResponse ),
      getJolieNativeTypeFromOpenApiNativeType( GetJolieNativeTypeFromOpenApiNativeTypeRequest )( string ),
      getReferenceName( string )( string )
}
