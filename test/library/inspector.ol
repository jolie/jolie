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
      if( !.isNative ){
        throw( TestFailed, "Type " + expectedTypeName + " is a native type" )
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
    .documentation = "Backward block\n                          node documentation";
    .name = "outputPortName"
  };
  with( expectedTypeSubtype[2] ){
      .documentation = "forward block node documentation";
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
  
  undef( response );

  expectedType.filename = "library/private/inspector/types_5.ol";
  expectedTypeName = "myChoice";
  expectedTypeDocumentation = "backward comment choice";
  inspectTypes@Inspector( expectedType )( response );
  with ( response.type ){
    if( !.isChoice ){
      throw( TestFailed, "Type " + expectedTypeName + " is not marked as a type choice" )
    };
    if( .isNative ){
      throw( TestFailed, "Type " + expectedTypeName + " is not native" )
    };
    trim@StringUtils( .documentation )( .documentation );
    if( .documentation != expectedTypeDocumentation ){
      throw( TestFailed, "Type " + expectedTypeName + " has a wrong documentation: " + .documentation )
    };
    if( .subtype[0].rootType != "void" ){
      throw( TestFailed, "Subtype 0 of " + expectedTypeName + " has an unexpected rootType: " + .subtype[0].rootType )

    };
    trim@StringUtils( .subtype[0].subtype[0].documentation[0] )( .subtype[0].subtype[0].documentation[0] );
    if( .subtype[0].subtype[0].documentation[0] = "first choice, fwd" ){
      throw( TestFailed, "Subtype[0].Subtype[0] of " + expectedTypeName + " has a wrong documentation: " + .subtype[0].subtype[0].documentation[0] )
    };
    if( .subtype[0].subtype[0].name != "a" ){
      throw( TestFailed, "Subtype[0].Subtype[0] of " + expectedTypeName + " has a wrong node: " + .subtype[0].subtype[0].name )
    };
    if( .subtype[0].subtype[1].name != "b" ){
      throw( TestFailed, "Subtype[0].Subtype[1] of " + expectedTypeName + " has a wrong node: " + .subtype[0].subtype[1].name )
    };
    if( .subtype[0].subtype[1].subtype[0].name != "c" ){
      throw( TestFailed, "Subtype[0].Subtype[1].Subtype[0] of " + expectedTypeName + " has a wrong node: " + .subtype[0].subtype[1].subtype[0].name )
    };
    if( !.subtype[0].subtype[1].subtype[0].undefinedSubtypes ){
      throw( TestFailed, "Subtype[0].Subtype[1].Subtype[0] of " + expectedTypeName + " should have undefined subtypes" )
    };
    trim@StringUtils( .subtype[0].subtype[1].subtype[0].documentation )( .subtype[0].subtype[1].subtype[0].documentation );
    if( .subtype[0].subtype[1].subtype[0].documentation != "first choice, nested, bwd" ){
      throw( TestFailed, "Subtype[0].Subtype[1].Subtype[0] of " + expectedTypeName + " has a wrong documentation: " + .subtype[0].subtype[1].subtype[0].documentation )
    };
    if( !.subtype[1].isChoice ){
      throw( TestFailed, "Subtype 1 of " + expectedTypeName + " should be marked as a choice" )
    };
    nestedDoc -> .subtype[1].subtype[1].subtype[0].subtype[1].subtype[1].subtype[0].documentation;
    trim@StringUtils( nestedDoc )( nestedDoc );
    if( nestedDoc != "very, nested, bwd comment" ){
      throw( TestFailed, "Subtype[1].Subtype[1].Subtype[1].Subtype[0] of " + expectedTypeName + " has a wrong documentation: " + nestedDoc )
    }    
  }

  undef( response );
  
  expectedType.filename = "library/private/inspector/types_6.ol";
  inspectProgram@Inspector( expectedType )( response );
  with( response ){
    if( .port[0].isOutput ){
      throw( TestFailed, "Port[0] should be marked as an inputPort" )
    };
    if( .port[0].name != "MyInput" ){
      throw( TestFailed, "Port[0] has a wrong name" )
    };
    trim@StringUtils( .port[0].documentation )( .port[0].documentation );
    if( .port[0].documentation != "bwc port documentation" ){
      throw( TestFailed, "Port[0] has a wrong name: " + .port[0].documentation )
    };
    if( .port[0].interface[0].name != "MyInterface" ){
      throw( TestFailed, "Port[0].interface[0] has a wrong name: " + .port[0].interface[0].name )
    };
    d -> .port[0].interface[0].operation[0].documentation;
    trim@StringUtils( d )( d );
    if( d != "a backward comment for the request" ){
      throw( TestFailed, "Port[0].interface[0].operation[0] has a wrong documentation: " + d )
    };
    d -> .port[0].interface[0].operation[2].documentation;
    trim@StringUtils( d )( d );
    if( d != "request-response op1 documentation" ){
      throw( TestFailed, "Port[0].interface[0].operation[2] has a wrong documentation: " + d )
    };
    if( .port[0].interface[0].operation[2].fault[1].name != "MyOtherFault" ){
      throw( TestFailed, "Port[0].interface[0].operation[2].fault[1] has a wrong name: " + .port[0].interface[0].operation[2].fault[1].name )
    }
  }
}