include "../AbstractTestUnit.iol"

include "plain_xml_manager.iol"
include "xml_utils.iol"
include "string_utils.iol"
include "file.iol"
include "console.iol"

define doTest
{
      rq.filename = "./includedServices/test.xml";
      readFile@File( rq )( xml );
      xmlToPlainValue@XmlUtils( xml )( plainDriverXML );
      with( resource ) {
          .resourceName = "test";
          .xml = xml
      };
      createPlainXML@PlainXMLManager( resource )();
      undef( resource.xml );
      getXMLString@PlainXMLManager( resource )( xml_string );
      length@StringUtils( xml_string )( length );
      if ( length != 7922 ) {
          println@Console( "Detected length: " + length )();
          println@Console( "Expected lenght: 800" )();
          throw( TestFailed )
      }
}
