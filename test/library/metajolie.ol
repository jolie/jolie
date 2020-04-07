include "../AbstractTestUnit.iol"
include "metajolie.iol"
include "string_utils.iol"
include "console.iol"
include "time.iol"

define doTest
{
  sleep@Time( 1000 )()
  with( rq ) {
      .filename = "private/sample_service.ol"
  };
  getInputPortMetaData@MetaJolie( rq )( meta_description );
  if ( #meta_description.input != 1 ) {
      throw( TestFailed, "Expected 1 input port, found " + #meta_description.input )
  };
  if ( meta_description.input.name != "TPort" ) {
      throw( TestFailed, "Expected input port name equal to \"TPort\", found " + meta_description.input.name )
  };
  if ( meta_description.input[0].protocol != "sodep" ) {
      throw( TestFailed, "Expected sodep protocol, found " + meta_description.input[0].protocol )
  };
  if ( #meta_description.input.interfaces != 1 ) {
      throw( TestFailed, "Expected 1 interface, found " + #meta_description.input.interfaces )
  };
  if ( meta_description.input.interfaces.name != "TmpInterface" ) {
      throw( TestFailed, "Expected interface name equal to \"TmpInterface\", found " + meta_description.input.interfaces.name )
  };
  if ( meta_description.input.interfaces.documentation != " documentation of interface " ) {
      throw( TestFailed, "Expected interface documentation equal to \" documentation of interface \", found " + meta_description.input.interfaces.documentation )
  };
  if ( #meta_description.input.interfaces.types != 7 ) {
      throw( TestFailed, "Expected 7 types, found " + #meta_description.input.interfaces.types )
  };
  if ( #meta_description.input.interfaces.operations != 3 ) {
      throw( TestFailed, "Expected 3 operation, found " + #meta_description.input.interfaces.operations )
  };
  if ( meta_description.input.interfaces.operations.operation_name != "tmp" ) {
      throw( TestFailed, "Expected operation_name equal to \"tmp\", found " + meta_description.input.interfaces.operations.operation_name )
  };
  if ( meta_description.input.interfaces.operations[1].operation_name != "tmp2" ) {
      throw( TestFailed, "Expected second operation_name equal to \"tmp2\", found " + meta_description.input.interfaces.operations[1].operation_name )
  }
  if ( meta_description.input.interfaces.operations[2].operation_name != "tmp3" ) {
      throw( TestFailed, "Expected second operation_name equal to \"tmp3\", found " + meta_description.input.interfaces.operations[1].operation_name )
  }
  ops -> meta_description.input.interfaces.operations
  for( o = 0, o < #ops, o++ ) {
      if ( ops[ o ].operation_name == "tmp" ) {
          if ( ops[ o ].documentation != "documentation of operation tmp" ) {
              throw( TestFailed, "Expected documentation for operation tmp to be \"documentation of operation tmp\", found \"" + ops[ o ].documentation + "\"")
          }
      }
      if ( ops[ o ].operation_name == "tmp2" ) {
          if ( ops[ o ].documentation != "documentation of operation tmp2" ) {
              throw( TestFailed, "Expected documentation for operation tmp2 to be \"documentation of operation tmp2\", found \"" + ops[ o ].documentation + "\"")
          }
      }
      if ( ops[ o ].operation_name == "tmp3" ) {
          if ( ops[ o ].documentation != "" ) {
              throw( TestFailed, "Expected no documentation for operation tmp3, found " + ops[ o ].documentation )
          }
      }

      if ( !is_defined( ops[ o ].fault ) ) {
          throw( TestFailed, "Expected faults in operation " + ops[ o ].operation_name )
      }
      if ( ops[ o ].fault.name == "Fault2" && ops[ o ].fault.type.undefined != true ) {
          valueToPrettyString@StringUtils( ops[ o ].fault )( f )
          throw( TestFailed, "Fault2 must be undefined  .undefined = true, found " + f )
      }
  }

  for( tp in meta_description.input.interfaces.types ) {
      if ( tp.name == "T2" ) {
          if ( tp.documentation != "documentation of type T2" ) {
              throw( TestFailed, "Expected documentation for type T2 it should be \"documentation of type T2\", found \"" + tp.documentation + "\"" )
          }
          if ( tp.type.sub_type.documentation != "documentation of field") {
              throw( TestFailed, "Expected documentation for field of type T2 it should be \"documentation of field\", found \"" + tp.type.sub_type.documentation + "\"")
          }
      }

  }
  getOutputPortMetaData@MetaJolie( rq )( meta_description );
  if ( #meta_description.output != 1 ) {
      throw( TestFailed, "Expected 1 output port, found " + #meta_description.output )
  };

  getMetaData@MetaJolie( rq )( metadata )
  if ( #metadata.types != 11 ) {
      throw( TestFailed, "Expected 11 types in metadata, found " + #metadata.types )
  }
  for( t = 0, t < #metadata.types, t++ ) {
      if ( metadata.types[ t ].name == "T7" && metadata.types[ t ].type.undefined != true ) {
          valueToPrettyString@StringUtils( metadata.types[ t ] )( tt )
          throw( TestFailed, "Type T7 must be undefined  .undefined = true, found " + tt )
      }
  }
  if ( #metadata.interfaces != 2 ) {
      throw( TestFailed, "Expected 2 interface in metadata, found " + #metadata.interfaces )
  }
   if ( #metadata.interfaces.operations != 6 ) {
      throw( TestFailed, "Expected 6 operations in metadata.interfaces[0], found " + #metadata.interfaces.operations )
  }
  if ( #metadata.input != 1 ) {
      throw( TestFailed, "Expected 1 input in metadata, found " + #metadata.input )
  }
  if ( #metadata.communication_dependencies != 1 ) {
      throw( TestFailed, "Expected 1 communication_dependencies in metadata, found " + #metadata.communication_dependencies )
  }
  mcom -> metadata.communication_dependencies
  if ( mcom.input_operation.name != "tmp" || mcom.input_operation.type != "RequestResponse" ) {
      throw( TestFailed, "Expected  communication_dependencies input_operation tmp of type RequestRepsponse in metadata, found " + mcom.input_operation.name + "," + mcom.input_operation.type )
  }
  if ( #mcom.dependencies != 1 ) {
      throw( TestFailed, "Expected 1 dependencies in communication_dependencies metadata, found " + #mcom.dependencies )
  }
  if ( mcom.dependencies.name != "print" || mcom.dependencies.port != "Console" || mcom.dependencies.type != "SolicitResponse" ) {
      throw( TestFailed, "Wrong dependencies in communication_dependencies metadata, expected print@Console found " +  mcom.dependencies.name + "," +  mcom.dependencies.type + "," +  mcom.dependencies.port )
  }

  getNativeTypeStringList@MetaJolie()( ntype_list )
  if ( #ntype_list.native_type != 8 ) {
      throw( TestFailed, "Expected 8 native types found " + #ntype_list.native_type )
  }
  for( t in ntype_list.native_type ) {
      checkNativeType@MetaJolie({ .type_name = t } )( is_native )
      if ( !is_native.result ) {
          throw( TestFailed, "Native Type " + t + " retrieved from getNativeTypeStringList is not native" )
      } else {
          getNativeTypeFromString@MetaJolie({ .type_name = t } )( ntype )
          if ( !is_defined( ntype.( t + "_type" ) ) ) {
              valueToPrettyString@StringUtils( ntype )( s )
              throw( TestFailed, "getNativeTypeFromString does not return the correct native type for Native Type " + t + ", got " + s )
          }
      }
  }

  // comparing nodes
  a.b.c.d.m.n.l.o = 1
  a.b.c.d.m.n.l = 2
  a.b.c.d.m.n = 3
  a.b.c.d = 4
  a.b.c = 5

  z.b.c.d.m.n.l.o = 1
  z.b.c.d.m.n.l = 2
  z.b.c.d.m.n = 3
  z.b.c.d = 4
  z.b.c = 5

  with( comp_rq ) {
      .v1 -> a;
      .v2 -> z
  }
  scope( compare_values ) {
      install( ComparisonFailed => throw( TestFailed, compare_values.ComparisonFailed ) )
      compareValuesStrict@MetaJolie( comp_rq )()
  }

  with( comp_rq ) {
      .v1 -> z;
      .v2 -> a
  }
  scope( compare_values ) {
      install( ComparisonFailed => throw( TestFailed, compare_values.ComparisonFailed ) )
      compareValuesStrict@MetaJolie( comp_rq )()
  }

  scope( compare_values ) {
      install( ComparisonFailed => throw( TestFailed, compare_values.ComparisonFailed ) )
      compareValuesVectorLight@MetaJolie( comp_rq )()
  }

  undef ( a.b.c )
  scope( compare_values ) {
      install( ComparisonFailed => nullProcess )
      compareValuesStrict@MetaJolie( comp_rq )()
      throw( TestFailed, "Expected values are different but comparison succeeded" )
  }

  // test aggregation with extender 
   with( rq ) {
      .filename = "private/sample_service2.ol"
   };
   getInputPortMetaData@MetaJolie( rq )( meta_description )

}
