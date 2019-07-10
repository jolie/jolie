include "../AbstractTestUnit.iol"
include "inspector.iol"
include "string_utils.iol"
include "console.iol"

define doTest
{
	expectedType.filename = "library/private/inspector/types_1.ol"
	expectedTypeName = "myType"
	expectedTypeDocumentation = "fwd 1"
	inspectTypes@Inspector( expectedType )( response )
	if( #response.types > 1 ){
		throw( TestFailed, "Program types_1.ol contains only one type, found " + #response.types )
	}
	t -> response.types
	if( t.isChoice ){
		throw( TestFailed, "Type " + expectedTypeName + " is not a type choice" )
	}
	if( t.name != expectedTypeName ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	}
	if( t.nativeType != "void" ){
		throw( TestFailed, "Type " + expectedTypeName + " should have 'void' as native type, found: " + t.nativeType )
	}
	if( #t.fields > 0 ){
		throw( TestFailed, "Type " + expectedTypeName + " should have no fields, found " + #t.fields )
	}
	if( t.untypedFields ){
		throw( TestFailed, "Type " + expectedTypeName + " should have no untypedFields" )
	}

	undef( response )

	expectedType.filename = "library/private/inspector/types_2.ol"
	expectedTypeName = "myType2"
	expectedTypeDocumentation = "bwd 2"
	inspectTypes@Inspector( expectedType )( response )
	t -> response.types
	if( t.name != expectedTypeName ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	}

	undef( response )

	expectedType.filename = "library/private/inspector/types_3.ol"
	expectedTypeName = "SetRedirectionRequest"
	expectedTypeDocumentation = "backward comment"

	e -> expFields[ 0 ] 
	e.name = "inputPortName"
	e.documentation = "Backward node documentation"
	e.range.min = 1
	e.range.max = 5
	e.type.nativeType = "string"
	
	e -> expFields[ 1 ]
	e.name = "outputPortName"
	e.documentation = "Backward block\n                          node documentation"
	e.range.min = 0
	e.range.min = 2147483647 // MAX_INTEGER
	e.type.nativeType = "string"

	e -> expFields[ 2 ]
	e.name = "resourceName"
	e.documentation = "forward block node documentation"
	e.range.min = 0
	e.range.min = 1
	e.type.nativeType = "string"

	inspectTypes@Inspector( expectedType )( response )
	t -> response.types
	if( t.name != expectedTypeName ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	}
	field -> t.fields[i]
	expField -> expFields[i]
	if( #t.fields != #expFields ){
		throw( TestFailed, "Type " + expField.name + " has unexpected fields, expected: " + #expFields + " found " + #t.fields )
	}
	for ( i=0, i<#expectedFields, i++ ) {
		if( field.name != expField.name ){
			throw( TestFailed, "Type " + expField.name + " has a wrong name: " + fields.name )
		}
		if( field.range != expField.name ){
			throw( TestFailed, "Type " + expField.name + " has a wrong name: " + fields.name )
		}
		trim@StringUtils( field.type.documentation )( field.type.documentation )
		if( field.type.documentation != expField.documentation ){
			throw( TestFailed, "Type " + expField.name + " has a wrong documentation: " + field.type.documentation )
		} 
	}

	undef( response )

	expectedType.filename = "library/private/inspector/types_4.ol"
	expectedTypeName = "SetOutputPortRequest"
	expectedTypeDocumentation = "forward comment for SetOutputPortRequest"
	inspectTypes@Inspector( expectedType )( response )
	t -> response.types
	if( t.name != expectedTypeName ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	}
	expectedFieldName = "name"
	expectedFieldDocumentation = "bwd comment"
	t -> response.types.type.fields
	if( t.name != expectedFieldName ){
		throw( TestFailed, "Field " + expectedFieldName + " has a wrong name: " + t.name )
	}
	t -> response.types.type.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedFieldDocumentation ){
		throw( TestFailed, "Field " + expectedFieldName + " has a wrong documentation: " + t.documentation )
	}
	expectedFieldName = "location"
	expectedFieldDocumentation = "fwd The location of the output port"
	t -> response.types.type.fields.type.fields
	if( t.name != expectedFieldName ){
		throw( TestFailed, "Field " + expectedFieldName + " has a wrong name: " + t.name )
	}
	t -> response.types.type.fields.type.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedFieldDocumentation ){
		throw( TestFailed, "Field " + expectedFieldName + " has a wrong documentation: " + t.documentation )
	}
	expectedFieldName = "protocol"
	expectedFieldDocumentation = "The name of the protocol (e.g., sodep, http)"
	t -> response.types.type.fields.type.fields.type.fields
	if( t.name != expectedFieldName ){
		throw( TestFailed, "Field " + expectedFieldName + " has a wrong name: " + t.name )
	}
	t -> response.types.type.fields.type.fields.type.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedFieldDocumentation ){
		throw( TestFailed, "Field " + expectedFieldName + " has a wrong documentation: " + t.documentation )
	}
	if( !t.untypedFields ){
		throw( TestFailed, "Field " + expectedFieldName + " should have untypedFields" )
	}

	undef( response )

	expectedType.filename = "library/private/inspector/types_5.ol"
	expectedTypeName = "myChoice"
	expectedTypeDocumentation = "backward comment choice"
	inspectTypes@Inspector( expectedType )( response )
	t -> response.types.type;
	// if( !( is_defined( t.left ) && is_defined( t.right ) ) ){
	// 	throw( TestFailed, "Type " + expectedTypeName + " is not marked as a type choice" )
	// }
	// trim@StringUtils( t.documentation )( t.documentation )
	// if( t.documentation != expectedTypeDocumentation ){
	// 	throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	// }
	// t -> response.types.type.left
	// if( t.nativeType != "void" ){
	// 		throw( TestFailed, "Left type of " + expectedTypeName + " has nativeType: " + t.nativeType + ", expected void" )
	// }
	// t -> response.types.type.left.field[ 0 ]
	// if( t.name != "a" ){
	// 	throw( TestFailed, "field[0] of the left type of " + expectedTypeName + " has a wrong name: " + t.name )
	// }
	// t -> response.types.type.left.field[ 0 ].type
	// trim@StringUtils( t.documentation )( t.documentation )
	// if( t.documentation = "first choice, fwd" ){
	// 	throw( TestFailed, "field[0] of the left type of " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	// }
	t -> response.types.type.left.fields[ 1 ]
	valueToPrettyString@StringUtils( t )( s ); println@Console( s )()
	if( t.name != "b" ){
		throw( TestFailed, "fields[1] of the left type of " + expectedTypeName + " has a wrong name: " + t.name )
	}
	t -> response.types.type.left.fields[ 1 ].type.fields
	if( t.name != "c" ){
		throw( TestFailed, "fields[ 1 ].field[ 0 ] of the left type of " + expectedTypeName + " has a wrong name: " + t.name )
	}
	t -> response.types.type.left.fields[ 1 ].type.fields.type
	if( !t.untypedFields ){
		throw( TestFailed, "fields[ 1 ].field[ 0 ]  of the left type of " + expectedTypeName + " should have untyped fields" )
	}
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "first choice, nested, bwd" ){
		throw( TestFailed, "fields[ 1 ].field[ 0 ]  of the left type of " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	}
	t -> response.types.type.right
	if( !( is_defined( t.left ) && is_defined( t.right ) ) ){
		throw( TestFailed, "Right type of " + expectedTypeName + " should be marked as a choice" )
	}
	t -> response.types.type.right.right.fields.type.right.right.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "very, nested, bwd comment" ){
		throw( TestFailed, "Field of right of right of field of right of right of " + expectedTypeName + " has a wrong documentation: " + t.documentation )
	}

	// undef( response )

	// expectedType.filename = "library/private/inspector/types_6.ol"
	// inspectPorts@Inspector( expectedType )( response )
	// with( response ){
	// 	if( .port[0].isOutput ){
	// 		throw( TestFailed, "Port[0] should be marked as an inputPort" )
	// 	}
	// 	if( .port[0].name != "MyInput" ){
	// 		throw( TestFailed, "Port[0] has a wrong name" )
	// 	}
	// 	trim@StringUtils( .port[0].documentation )( .port[0].documentation )
	// 	if( .port[0].documentation != "bwc port documentation" ){
	// 		throw( TestFailed, "Port[0] has a wrong name: " + .port[0].documentation )
	// 	}
	// 	if( .port[0].interface[0].name != "MyInterface" ){
	// 		throw( TestFailed, "Port[0].interface[0] has a wrong name: " + .port[0].interface[0].name )
	// 	}
	// 	d -> .port[0].interface[0].operation[0].documentation
	// 	trim@StringUtils( d )( d )
	// 	if( d != "a backward comment for the request" ){
	// 		throw( TestFailed, "Port[0].interface[0].operation[0] has a wrong documentation: " + d )
	// 	}
	// 	d -> .port[0].interface[0].operation[2].documentation
	// 	trim@StringUtils( d )( d )
	// 	if( d != "request-response op1 documentation" ){
	// 		throw( TestFailed, "Port[0].interface[0].operation[2] has a wrong documentation: " + d )
	// 	}
	// 	if( .port[0].interface[0].operation[2].fault[1].name != "MyOtherFault" ){
	// 		throw( TestFailed, "Port[0].interface[0].operation[2].fault[1] has a wrong name: " + .port[0].interface[0].operation[2].fault[1].name )
	// 	}
	// }
}