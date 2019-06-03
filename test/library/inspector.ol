include "../AbstractTestUnit.iol"
include "inspector.iol"
include "string_utils.iol"
include "console.iol"

define doTest
{
  expectedType.filename = "library/private/inspector/types_1.ol";
  expectedTypeName = "myType";
  expectedTypeDocumentation = "fwd 1";
  inspectTypes@Inspector( expectedType )( response );
  with( response.type ){
      if( .isChoice ){
        throw( TestFailed, "Type " + expectedTypeName + " is not a type choice" )
      };
      if( .isNative ){
        throw( TestFailed, "Type " + expectedTypeName + " is not a native type" )
      };
      if( .name != expectedTypeName ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + .name )
      };
      trim@StringUtils( .documentation )( .documentation );
      if( .documentation != expectedTypeDocumentation ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + .documentation )
      }
  };

  undef( response );

  expectedType.filename = "library/private/inspector/types_2.ol";
  expectedTypeName = "myType2";
  expectedTypeDocumentation = "bwd 2";
  inspectTypes@Inspector( expectedType )( response );
  with( response.type ){
      if( .name != expectedTypeName ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + .name )
      };
      trim@StringUtils( .documentation )( .documentation );
      if( .documentation != expectedTypeDocumentation ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + .documentation )
    }
  };

  undef( response );

  expectedType.filename = "library/private/inspector/types_3.ol";
  expectedTypeName = "SetRedirectionRequest";
  expectedTypeDocumentation = "backward comment";
  with( expectedTypeSubtype[0] ){
    .documentation = "Backward node documentation";
    .name = "inputPortName"
  };
  with( expectedTypeSubtype[1] ){
    .documentation = "Another backward\n                          node documentation";
    .name = "outputPortName"
  };
  with( expectedTypeSubtype[2] ){
      .documentation = "forward node documentation";
      .name = "resourceName"
  };
  inspectTypes@Inspector( expectedType )( response );
  with( response.type ){
      if( .isChoice ){
        throw( TestFailed, "Type " + expectedTypeName + " is not a type choice" )
      };
      if( .isNative ){
        throw( TestFailed, "Type " + expectedTypeName + " is not a native type" )
      };
      if( .name != expectedTypeName ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + .name )
      };
      trim@StringUtils( .documentation )( .documentation );
      if( .documentation != expectedTypeDocumentation ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + .documentation )
      }
      subtype -> .subtype[i];
      expSubtype -> expectedTypeSubtype[i];
      for ( i=0, i<#expectedTypeSubtype, i++ ) {
        if( subtype.name != expSubtype.name ){
          throw( TestFailed, "Type " + expSubtype.name + " has a wrong name: " + subtype.name )
        };
        trim@StringUtils( subtype.documentation )( subtype.documentation );
        if( subtype.documentation != expSubtype.documentation ){
          throw( TestFailed, "Type " + expSubtype.name + " has a wrong documentation: " + subtype.documentation )
        } 
      }
  };

  undef( response );

  expectedType.filename = "library/private/inspector/types_4.ol";
  expectedTypeName = "SetOutputPortRequest";
  expectedTypeDocumentation = "forward comment for SetOutputPortRequest";
  inspectTypes@Inspector( expectedType )( response );
  with( response.type ){
      if( .isChoice ){
        throw( TestFailed, "Type " + expectedTypeName + " is not a type choice" )
      };
      if( .isNative ){
        throw( TestFailed, "Type " + expectedTypeName + " is not a native type" )
      };
      if( .name != expectedTypeName ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong name: " + .name )
      };
      trim@StringUtils( .documentation )( .documentation );
      if( .documentation != expectedTypeDocumentation ){
        throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + .documentation )
      };
      expectedSubtypeName = "name";
      expectedSubtypeDocumentation = "bwd comment";
      with( .subtype ){
        if( .name != expectedSubtypeName ){
          throw( TestFailed, "Type " + expectedSubtypeName + " has a wrong name: " + .name )
        };
        trim@StringUtils( .documentation )( .documentation );
        if( .documentation != expectedSubtypeDocumentation ){
          throw( TestFailed, "Type " + expectedSubtypeName + " has a wrong documentation: " + .documentation )
        };
        expectedSubtypeName = "location";
        expectedSubtypeDocumentation = "fwd The location of the output port";
        with( .subtype ){
          if( .name != expectedSubtypeName ){
            throw( TestFailed, "Type " + expectedSubtypeName + " has a wrong name: " + .name )
          };
          trim@StringUtils( .documentation )( .documentation );
          if( .documentation != expectedSubtypeDocumentation ){
            throw( TestFailed, "Type " + expectedSubtypeName + " has a wrong documentation: " + .documentation )
          };
          expectedSubtypeName = "protocol";
          expectedSubtypeDocumentation = "The name of the protocol (e.g., sodep, http)";
          with( .subtype ){
            if( .name != expectedSubtypeName ){
              throw( TestFailed, "Type " + expectedSubtypeName + " has a wrong name: " + .name )
            };
            trim@StringUtils( .documentation )( .documentation );
            if( .documentation != expectedSubtypeDocumentation ){
              throw( TestFailed, "Type " + expectedSubtypeName + " has a wrong documentation: " + .documentation )
            }
          }
        }
      }
  }
  
  // valueToPrettyString@StringUtils( response )( s );
  // println@Console( s )()

}