include "../AbstractTestUnit.iol"
include "xml_utils.iol"

define doTest
{
	xml = "
<people>
	<person>
		<firstName>John</firstName>
		<lastName>Smith</lastName>
	</person>
	<person>
		<firstName>Donald</firstName>
		<lastName>Duck</lastName>
	</person>
</people>
	";
	xmlToValue@XmlUtils( xml )( v );
	if ( #v.person != 2 ) {
		throw( TestFailed, "xmlToValue: invalid vector size" )
	} else if ( v.person[0].firstName != "John" ) {
		throw( TestFailed, "xmlToValue: first person's firstName is wrong" )
	} else if ( v.person[1].lastName != "Duck" ) {
		throw( TestFailed, "xmlToValue: second person's lastName is wrong" )
	}
}
