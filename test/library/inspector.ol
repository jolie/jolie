include "../AbstractTestUnit.iol"
include "inspector.iol"
include "string_utils.iol"
include "console.iol"

define doTest
{
  testType.filename = "library/private/inspector/types.ol";
  // inspectTypes@Inspector( testType )( response );
  inspectTypes@Inspector( { filename = "library/private/inspector/types.ol" } )( response );
  valueToPrettyString@StringUtils( response )( s );
  println@Console( s )()
}
