include "../AbstractTestUnit.iol"
include "inspector.iol"
include "string_utils.iol"
include "console.iol"

define resetAliases {
	undef( t )
	undef( response )
}

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
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
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

	resetAliases // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	expectedType.filename = "library/private/inspector/types_2.ol"
	expectedTypeName = "myType2"
	expectedTypeDocumentation = "bwd 2"
	inspectTypes@Inspector( expectedType )( response )
	t -> response.types
	if( t.name != expectedTypeName ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
	}

	resetAliases // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

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

	undef( e )

	inspectTypes@Inspector( expectedType )( response )
	t -> response.types
	if( t.name != expectedTypeName ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
	}
	field -> t.fields[i]
	expField -> expFields[i]
	if( #t.fields != #expFields ){
		throw( TestFailed, "Type " + expField.name + " has unexpected fields, expected: " + #expFields + " found " + #t.fields )
	}
	for ( i=0, i<#expectedFields, i++ ) {
		if( field.name != expField.name ){
			throw( TestFailed, "Type " + expField.name + " has an unexpected name: " + fields.name )
		}
		if( field.range != expField.name ){
			throw( TestFailed, "Type " + expField.name + " has an unexpected name: " + fields.name )
		}
		trim@StringUtils( field.type.documentation )( field.type.documentation )
		if( field.type.documentation != expField.documentation ){
			throw( TestFailed, "Type " + expField.name + " has an unexpected documentation: " + field.type.documentation )
		} 
	}

	resetAliases // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	expectedType.filename = "library/private/inspector/types_4.ol"
	expectedTypeName = "SetOutputPortRequest"
	expectedTypeDocumentation = "forward comment for SetOutputPortRequest"
	inspectTypes@Inspector( expectedType )( response )
	t -> response.types
	if( t.name != expectedTypeName ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
	}
	expectedFieldName = "name"
	expectedFieldDocumentation = "bwd comment"
	t -> response.types.type.fields
	if( t.name != expectedFieldName ){
		throw( TestFailed, "Field " + expectedFieldName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedFieldDocumentation ){
		throw( TestFailed, "Field " + expectedFieldName + " has an unexpected documentation: " + t.documentation )
	}
	expectedFieldName = "location"
	expectedFieldDocumentation = "fwd The location of the output port"
	t -> response.types.type.fields.type.fields
	if( t.name != expectedFieldName ){
		throw( TestFailed, "Field " + expectedFieldName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type.fields.type.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedFieldDocumentation ){
		throw( TestFailed, "Field " + expectedFieldName + " has an unexpected documentation: " + t.documentation )
	}
	expectedFieldName = "protocol"
	expectedFieldDocumentation = "The name of the protocol (e.g., sodep, http)"
	t -> response.types.type.fields.type.fields.type.fields
	if( t.name != expectedFieldName ){
		throw( TestFailed, "Field " + expectedFieldName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type.fields.type.fields.type.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedFieldDocumentation ){
		throw( TestFailed, "Field " + expectedFieldName + " has an unexpected documentation: " + t.documentation )
	}
	if( !t.untypedFields ){
		throw( TestFailed, "Field " + expectedFieldName + " should have untypedFields" )
	}

	resetAliases // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	expectedType.filename = "library/private/inspector/types_5.ol"
	expectedTypeName = "myChoice"
	expectedTypeDocumentation = "backward comment choice"
	inspectTypes@Inspector( expectedType )( response )
	t -> response.types.type
	if( !( is_defined( t.left ) && is_defined( t.right ) ) ){
		throw( TestFailed, "Type " + expectedTypeName + " is not marked as a type choice" )
	}
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != expectedTypeDocumentation ){
		throw( TestFailed, "Type " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
	}
	t -> response.types.type.left
	if( t.nativeType != "void" ){
			throw( TestFailed, "Left type of " + expectedTypeName + " has nativeType: " + t.nativeType + ", expected void" )
	}
	t -> response.types.type.left.fields // fields[ 0 ] crashes due to an infinite aliasing loop
	if( t.name != "a" ){
		throw( TestFailed, "field[0] of the left type of " + expectedTypeName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type.left.fields[ 0 ].type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation = "first choice, fwd" ){
		throw( TestFailed, "field[0] of the left type of " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
	}
	t -> response.types.type.left.fields[ 1 ]
	if( t.name != "b" ){
		throw( TestFailed, "fields[1] of the left type of " + expectedTypeName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type.left.fields[ 1 ].type.fields
	if( t.name != "c" ){
		throw( TestFailed, "fields[ 1 ].field[ 0 ] of the left type of " + expectedTypeName + " has an unexpected name: " + t.name )
	}
	t -> response.types.type.left.fields[ 1 ].type.fields.type
	if( !t.untypedFields ){
		throw( TestFailed, "fields[ 1 ].field[ 0 ] of the left type of " + expectedTypeName + " should have untyped fields" )
	}
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "first choice, nested, bwd" ){
		throw( TestFailed, "fields[ 1 ].field[ 0 ] of the left type of " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
	}
	t -> response.types.type.right
	if( !( is_defined( t.left ) && is_defined( t.right ) ) ){
		throw( TestFailed, "Right type of " + expectedTypeName + " should be marked as a choice" )
	}
	t -> response.types.type.right.right.fields.type.right.right.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "very, nested, bwd comment" ){
		throw( TestFailed, "Field of right of right of field of right of right of " + expectedTypeName + " has an unexpected documentation: " + t.documentation )
	}

	resetAliases // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	expectedType.filename = "library/private/inspector/types_6.ol"
	inspectPorts@Inspector( expectedType )( response )
	t -> response.inputPorts
	if( t.name != "MyInput" ){
		throw( TestFailed, "InputPort 'MyInput' has an unexpected name: " + t.name )
	}
	if( t.location != "local" ){
		throw( TestFailed, "InputPort 'MyInput' has an unexpected location: " + t.location )
	}
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "bwc port documentation" ){
		throw( TestFailed, "InputPort " + t.name + " has an unexpected documentation " + t.documentation )
	}
	t -> response.inputPorts.interfaces
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "backward interface documentation" ){
		throw( TestFailed, "Interface " + t.name + " has an unexpected documentation " + t.documentation )
	}
	t -> response.inputPorts.interfaces.operations
	if( #t != 4 ){
		throw( TestFailed, "Unexpected number of operations, found " + #t + ", expected 4" )
	}
	trim@StringUtils( t[0].documentation )( t[0].documentation )
	if( t[0].documentation != "a backward comment for the request" ){
		throw( TestFailed, "Operation " + t[0].name + " has an unexpected documentation " + t[0].documentation )
	}
	if( is_defined( t[1].responseType ) ){
		throw( TestFailed, "Operation " + t[1].name + " is unexpectedly marked as a RequestResponse with responseType " + t[1].responseType )
	}
	if( t[2].faults[0].name != "MyFault" || t[2].faults[1].name != "MyOtherFault" ){
		throw( TestFailed, "Operation " + t[2].name + " has unexpectedly fault names: " + t[2].faults[0].name + ", " + t[2].faults[1].name )
	}
	if( t[3].requestType != "void" || t[3].responseType != "void" ){
		throw( TestFailed, "Operation " + t[3].name + " has unexpectedly request or response types: " + t[3].requestType + ", " + t[3].responseType )
	}

	t -> response.outputPorts
	if( t.name != "MyOutput" ){
		throw( TestFailed, "OutputPort 'MyOutput' has an unexpected name: " + t.name )
	}
	if( is_defined( t.location ) || is_defined( t.protocol ) ){
		throw( TestFailed, "OutputPort 'MyOutput' should not define either a location or a protocol: " + t.location + ", " + t.protocol )
	}
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "bw block comment" ){
		throw( TestFailed, "OutputPort " + t.name + " has an unexpected documentation " + t.documentation )
	}
	t -> response.outputPorts.interfaces
	trim@StringUtils( t[0].documentation )( t[0].documentation )
	if( t[0].documentation != "interface 2 documentation" ){
		throw( TestFailed, "Interface " + t[0].name + " has an unexpected documentation " + t[0].documentation )
	}
	if( t[1].name != "MyInterface3" ){
		throw( TestFailed, "Interface 'MyInterface3' has an unexpected name " + t[1].name )
	}
	t -> response.outputPorts.interfaces[0].operations
	if( #t != 2 ){
		throw( TestFailed, "Unexpected number of operations, found " + #t + ", expected 2" )
	}
	trim@StringUtils( t[0].documentation )( t[0].documentation )
	trim@StringUtils( t[1].documentation )( t[1].documentation )
	if( t[0].documentation != t[1].documentation ){
		throw( TestFailed, "Documentation of operations " + t[0].name + " and " + t[1].name + " should coincide. Found: \n - " + t[0].documentation + "\n - " + t[1].documentation  )
	}

	t -> response.referredTypes
	if( t.name != "MyType" ){
		throw( TestFailed, "Unexpected name of referred type. Found: " + t.name )
	}
	t -> response.referredTypes.type.fields.type
	trim@StringUtils( t.documentation )( t.documentation )
	if( t.documentation != "a nice field" ){
		throw( TestFailed, "Unexpected documentation in field of referred type. Found: " + t.documentation )
	}

}