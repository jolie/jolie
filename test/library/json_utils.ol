from ..test-unit import TestUnitInterface
from json-utils import JsonUtils
from string-utils import StringUtils
from console import Console


service Main {

    embed JsonUtils as JsonUtils
	embed StringUtils as StringUtils 
	embed Console as Console

	inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

	define check
	{
		if ( #v._ != 2 ) {
			throw( TestFailed, "getJsonValue: invalid vector size" )
		} else if ( v._[0].id != 123456789123456789L ) {
			throw( TestFailed, "getJsonValue: first person's id is wrong" )
		} else if ( v._[0].firstName != "John" ) {
			throw( TestFailed, "getJsonValue: first person's firstName is wrong" )
		} else if ( v._[0].lastName != "Döner" ) {
			throw( TestFailed, "getJsonValue: first person's lastName is wrong" )
		} else if ( v._[0].age != 30 ) {
			throw( TestFailed, "getJsonValue: first person's age is wrong" )
		} else if ( v._[0].size != 90.5 ) {
			throw( TestFailed, "getJsonValue: first person's size is wrong" )
		} else if ( v._[0].male != true ) {
			throw( TestFailed, "getJsonValue: first person is not male" )
		} else if ( v._[1].firstName != "Donald" ) {
			throw( TestFailed, "getJsonValue: second person's firstName is wrong" )
		} else if ( v._[1].lastName != "Duck" ) {
			throw( TestFailed, "getJsonValue: second person's lastName is wrong" )
		}
	}

	define check2
	{
		if ( #v != 2 ) {
			throw( TestFailed, "getJsonValue: invalid vector size" )
		} else if ( v[0].firstName != "John" ) {
			throw( TestFailed, "getJsonValue: first person's firstName is wrong" )
		} else if ( v[0].lastName != "Doe" ) {
			throw( TestFailed, "getJsonValue: first person's lastName is wrong" )
		} else if ( v[1].firstName != "Donald" ) {
			throw( TestFailed, "getJsonValue: second person's firstName is wrong" )
		} else if ( v[1].lastName != "Duck" ) {
			throw( TestFailed, "getJsonValue: second person's lastName is wrong" )
		}
	}

	define check3
	{
		if ( #v != 1 ) {
			throw( TestFailed, "getJsonValue: invalid vector size" )
		} else if ( v != "Hi" ) {
			throw( TestFailed, "getJsonValue: wrong value" )
		}
	}

	define check4
	{
		if (#v != 1 || #v._ != 1 || #v._._ != 2 ) {
			throw( TestFailed, "getJsonValue: invalid vector size" )
		} else if ( v._._[0] != "Hi" ) {
			throw( TestFailed, "getJsonValue: wrong value" )
		} else if ( v._._[1] != "Ho" ) {
			throw( TestFailed, "getJsonValue: wrong value" )
		}
	}


    main {
        test()() {


			json = "
			[
				{
					\"id\": 123456789123456789,
					\"firstName\": \"John\",
					\"lastName\": \"Döner\",
					\"age\": 30,
					\"size\": 90.5,
					\"male\": true
				},
				{
					\"firstName\": \"Donald\",
					\"lastName\": \"Duck\"
				}
			]
			"
			getJsonValue@JsonUtils( json )( v )
			check

			getJsonString@JsonUtils( v )( str )
			getJsonValue@JsonUtils( str )( v )
			check
			getJsonString@JsonUtils( v )( str2 )
			if ( str != str2 ) {
				throw( TestFailed, "getJsonString: JSON strings should match" )
			}

			undef( v )
			v[0] << { .firstName = "John", .lastName = "Doe" }
			v[1] << { .firstName = "Donald", .lastName = "Duck" }
			check2

			getJsonString@JsonUtils( v )( str )
			getJsonValue@JsonUtils( str )( v )
			check2
			getJsonString@JsonUtils( v )( str2 )
			if ( str != str2 ) {
				throw( TestFailed, "getJsonString: JSON strings should match" )
			};

			undef( v )
			v = "Hi"
			check3

			getJsonString@JsonUtils( v )( str )
			getJsonValue@JsonUtils( str )( v )
			check3
			getJsonString@JsonUtils( v )( str2 )
			if ( str != str2 ) {
				throw( TestFailed, "getJsonString: JSON strings should match" )
			};
			if ( str != "{\"$\":\"Hi\"}" ) {
				throw( TestFailed, "getJsonString: expected long root value" )
			};

			undef( v )
			json = "[[\"Hi\",\"Ho\"]]"
			getJsonValue@JsonUtils( json )( v )
			check4

			getJsonString@JsonUtils( v )( str )
			getJsonValue@JsonUtils( str )( v )
			check4
			getJsonString@JsonUtils( v )( str2 )
			if ( str != str2 ) {
				throw( TestFailed, "getJsonString: JSON strings should match" )
			}

			undef( v )
			getJsonString@JsonUtils( v )( str )
			getJsonValue@JsonUtils( str )( v )
			if ( !(v instanceof void) ) {
				throw( TestFailed, "getJsonValue: expected void" )
			}
			getJsonString@JsonUtils( v )( str2 )
			if ( str != str2 ) {
				throw( TestFailed, "getJsonString: JSON strings should match" )
			}
			if ( str != "{}" ) {
				throw( TestFailed, "getJsonString: expected null" )
			}

			// Null values

			json = "null"
			getJsonValue@JsonUtils( json )( v )
			if ( v != undefined ) {
				throw( TestFailed, "getJsonValue: expected undefined: null" )
			}
			json = "{}"
			getJsonValue@JsonUtils( json )( v )
			if ( v != undefined ) {
				throw( TestFailed, "getJsonValue: expected undefined: {}" )
			}
			json = "[]"
			getJsonValue@JsonUtils( json )( v )
			if ( v != undefined ) {
				throw( TestFailed, "getJsonValue: expected undefined: []" )
			}

			// Basic values

			json = "true"
			getJsonValue@JsonUtils( json )( v )
			if ( !(v instanceof bool) || v != true ) {
				throw( TestFailed, "getJsonValue: expected true" )
			}
			json = "10" // in JSON: long == int
			getJsonValue@JsonUtils( json )( v )
			if ( !(v instanceof int) || v != 10 ) {
				throw( TestFailed, "getJsonValue: expected 10" )
			}
			if ( !(v instanceof long) || v != 10L ) {
				throw( TestFailed, "getJsonValue: expected 10" )
			}
			json = "10.0"
			getJsonValue@JsonUtils( json )( v )
			if ( !(v instanceof double) || v != 10.0 ) {
				throw( TestFailed, "getJsonValue: expected 10.0" )
			}
			json = "\"Hi\""
			getJsonValue@JsonUtils( json )( v )
			if ( !(v instanceof string) || v != "Hi" ) {
				throw( TestFailed, "getJsonValue: expected \"Hi\"" )
			}


			validateJson@JsonUtils( {
				schema = "{"
				+ "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
				+ "  \"type\": \"object\","
				+ "  \"properties\": {"
				+ "    \"name\": { \"type\": \"string\" },"
				+ "    \"age\": { \"type\": \"integer\", \"minimum\": 18 }"
				+ "  },"
				+ "  \"required\": [\"name\"]"
				+ "}"
				json = "{"
				+ "  \"name\": \"Alice\","
				+ "  \"age\": 25"
				+ "}"
			})( validation )

			if ( is_defined( validation.validationMessage ) ) {
				valueToPrettyString@StringUtils( validation )( errors )
				throw( TestFailed, errors )
			}

			

		}
	}
}
