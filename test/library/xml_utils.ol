include "../AbstractTestUnit.iol"
include "xml_utils.iol"

define check
{
	if ( #v.person != 2 ) {
		throw( TestFailed, "xmlToValue: invalid vector size" )
	} else if ( v.person[0].id != 123456789123456789L ) {
		throw( TestFailed, "xmlToValue: first person's id is wrong" )
	} else if ( v.person[0].firstName != "John" ) {
		throw( TestFailed, "xmlToValue: first person's firstName is wrong" )
	} else if ( v.person[0].lastName != "Döner" ) {
		throw( TestFailed, "xmlToValue: first person's lastName is wrong" )
	} else if ( v.person[0].age != 30 ) {
		throw( TestFailed, "xmlToValue: first person's age is wrong" )
	} else if ( v.person[0].size != 90.5 ) {
		throw( TestFailed, "xmlToValue: first person's size is wrong" )
	} else if ( v.person[0].male != true ) {
		throw( TestFailed, "xmlToValue: first person is not male" )
	} else if ( v.person[1].firstName != "Donald" ) {
		throw( TestFailed, "xmlToValue: second person's firstName is wrong" )
	} else if ( v.person[1].lastName != "Duck" ) {
		throw( TestFailed, "xmlToValue: second person's lastName is wrong" )
	} else if ( v.person[1].empty != "" ) {
		throw( TestFailed, "xmlToValue: second person's empty is wrong" )
	}

}

define doTest
{
	xml = "
<people>
	<person>
		<id>123456789123456789</id>
		<firstName>John</firstName>
		<lastName>Döner</lastName>
		<age>30</age>
		<size>90.5</size>
		<male>true</male>
	</person>
	<person>
		<firstName>Donald</firstName>
		<lastName>Duck</lastName>
		<empty></empty>
	</person>
</people>
	";
	xmlToValue@XmlUtils( xml )( v );
	check;

	// Plain XML

	req.root -> v;
	req.rootNodeName = "people";
	req.plain = true;
	req.omitXmlDeclaration = true;
	valueToXml@XmlUtils( req )( str );
	xmlToValue@XmlUtils( str )( v );
	check;
	valueToXml@XmlUtils( req )( str2 );
	if ( str != str2 ) {
		throw( TestFailed, "xmlToValue: XML strings should match" )
	};

	// Storage XML

	undef( req );
	req.root -> v;
	req.rootNodeName = "people";
	valueToXml@XmlUtils( req )( str );
	xmlToValue@XmlUtils( str )( v );
	check;
	valueToXml@XmlUtils( req )( str2 );
	if ( str != str2 ) {
		throw( TestFailed, "xmlToValue: XML strings should match" )
	}
}
