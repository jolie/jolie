include "../AbstractTestUnit.iol"
include "metajolie.iol"
include "string_utils.iol"

define doTest
{
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
  if ( meta_description.input.interfaces.operations[1].operation_name != "tmp3" ) {
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
  getMetaData@MetaJolie( rq )( metadata )
  if ( #metadata.types != 7 ) {
      throw( TestFailed, "Expected 7 types in metadata, found " + #metadata.types )
  }
  for( t = 0, t < #metadata.types, t++ ) {
      if ( metadata.types[ t ].name == "T7" && metadata.types[ t ].type.undefined != true ) {
          valueToPrettyString@StringUtils( metadata.types[ t ] )( tt )
          throw( TestFailed, "Type T7 must be undefined  .undefined = true, found " + tt )
      }
  }
  if ( #metadata.interfaces != 1 ) {
      throw( TestFailed, "Expected 1 interface in metadata, found " + #metadata.interfaces )
  }
   if ( #metadata.interfaces.operations != 3 ) {
      throw( TestFailed, "Expected 3 operations in metadata.interfaces[0], found " + #metadata.interfaces.operations )
  }
  if ( #metadata.input != 1 ) {
      throw( TestFailed, "Expected 1 input in metadata, found " + #metadata.input )
  }
}
