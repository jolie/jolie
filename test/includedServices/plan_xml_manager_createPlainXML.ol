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
      ;

      /* getElement */
      nd.resourceName = "test";
      nd.path = "wsdl:definitions/wsdl:portType/wsdl:operation[0]/wsdl:documentation";
      getElement@PlainXMLManager( nd )( node );
      if( node.Name != "wsdl:documentation" ) {
          println@Console( "getElement failed" )();
          throw( TestFailed )
      }
      ;
      /* modifyElement */
      NEWTEXT = "test";
      NEWNODENAME = "testNodeName";
      node.Node.Text.Value = NEWTEXT;
      node.Name = NEWNODENAME;
      nd.content -> node;
      modifyElement@PlainXMLManager( nd )();
      undef( nd.content );
      nd.path = "wsdl:definitions/wsdl:portType/wsdl:operation[0]/" + NEWNODENAME;
      getElement@PlainXMLManager( nd )( node );
      if ( node.Node.Text.Value != NEWTEXT && node.Name != NEWNODENAME ) {
          println@Console( "modifyElement does not work, text node " + node.Node.Text.Value )();
          throw( TestFailed )
      }
      ;
      /* wrong path in getElement */
      scope( testPathNotFound ) {
        install( PathNotFound => nullProcess );
        nd.path = "wsdl:definitions/wsdl:portTypeXXX/wsdl:operation[0]/wsdl:documentation";
        getElement@PlainXMLManager( nd )( node );
        println@Console("PathNotFound not validated")();
        throw( TestFailed )
      }
      ;
      /* addResource */
      nd.resourceName = "test";
      nd.path = "wsdl:definitions/wsdl:portType";
      getElement@PlainXMLManager( nd )( node );
      with( add_resource_request ) {
        .resourceName = "test2";
        .resource.root << node
      };
      addResource@PlainXMLManager( add_resource_request )();
      nd.resourceName = "test2";
      nd.path = "wsdl:portType/wsdl:operation[0]/" + NEWNODENAME;
      getElement@PlainXMLManager( nd )( node );
      if( node.Name != NEWNODENAME ) {
          println@Console( "addResource failed" )();
          throw( TestFailed )
      }
}
