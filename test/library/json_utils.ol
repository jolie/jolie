include "../AbstractTestUnit.iol"
include "json_utils.iol"

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

define doTest
{
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
	";
	getJsonValue@JsonUtils( json )( v );
	check;

	getJsonString@JsonUtils( v )( str );
	getJsonValue@JsonUtils( str )( v );
	check;
	getJsonString@JsonUtils( v )( str2 );
	if ( str != str2 ) {
		throw( TestFailed, "getJsonValue: JSON strings should match" )
	}
}
