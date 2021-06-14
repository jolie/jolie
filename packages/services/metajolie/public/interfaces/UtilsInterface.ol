from .....types.definition_types import TypeDefinition
from .....types.definition_types import Type

type CheckOperationTypesRequest: void {
    t1: string {
        types*: TypeDefinition
    }
    t2: string {
        types*: TypeDefinition       
    }
}

type TypeLessThanRequest: void {
	t1 {
        type: Type
		types*: TypeDefinition
	}
	t2 {
        type: Type
		types*: TypeDefinition
	}
}

interface MetaJolieUtilsInterface {
    RequestResponse: 
        typeLessThan( TypeLessThanRequest )( bool ) throws TypeMissing( string ),
        checkOperationTypes( CheckOperationTypesRequest )( bool ) throws TypeMissing( string )       
}