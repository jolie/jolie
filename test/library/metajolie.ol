from ..test-unit import TestUnitInterface
from time import Time 
from string-utils import StringUtils
from metajolie import MetaJolie
from file import File
from console import Console
from metarender-code import MetaRenderCode

service Main {

    embed MetaJolie as MetaJolie
    embed StringUtils as StringUtils
    embed Time as Time
    embed File as File
    embed Console as Console
    embed MetaRenderCode as MetaRenderCode

    inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {

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
                if ( #metadata.types != 13 ) {
                    throw( TestFailed, "Expected 13 types in metadata, found " + #metadata.types )
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
                if ( #metadata.interfaces.operations != 8 ) {
                    throw( TestFailed, "Expected 8 operations in metadata.interfaces[0], found " + #metadata.interfaces.operations )
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
                for( intf in meta_description.input.interfaces ) {
                    if ( intf.name == "TmpInterface" ) {
                        for( t in intf.types ) {
                            if ( t.name == "T1" ) {
                                if ( t.type.sub_type.name != "ext" ) {
                                    throw( TestFailed, "Expected subnode ext in type T1" )
                                }
                            }
                            if ( t.name == "T2" ) {
                                for( s in t.type.sub_type ) {
                                    if ( s.name == "ext" ) {
                                        throw( TestFailed, "Subnode ext should not be declated in type T2" )
                                    }
                                }
                            }
                        }
                    }
                }


                // testing lessThan operations
                undef( rq )
                rq.filename = "private/sample_service6.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description6 )

                undef( rq )
                rq.i1 -> meta_description6.input.interfaces
                rq.i2 -> meta_description6.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result.result ) {
                    throw( TestFailed, "Expected true for interfaceDefinitionLessThan between the same interface of TmpInterface5" )
                }

                undef( rq )
                with( rq ) {
                    .filename = "private/sample_service3.ol"
                }
                getInputPortMetaData@MetaJolie( rq )( meta_description );
                undef( rq )
                rq.t1 = "T2" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T2"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result ) {
                    throw( TestFailed, "Expected true for typeDefinitionLessThan between the same type T2" )
                }


                rq.t2 = "T1"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( result ) {
                    throw( TestFailed, "Expected false for typeDefinitionLessThan between the same type T2 against T1" )
                }


                undef( rq )
                rq.t1 = "T5" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T7"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result ) {
                    throw( TestFailed, "Expected true for typeDefinitionLessThan between the same type T5 against T7" )
                }

                undef( rq )
                rq.t1 = "T1" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T6"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result ) {
                    throw( TestFailed, "Expected true for typeDefinitionLessThan between the same type T1 against T6" )
                }

                undef( rq )
                rq.t1 = "T6" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T1"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( result ) {
                    throw( TestFailed, "Expected false for typeDefinitionLessThan between the same type T6 against T1, found " + result )
                }

                undef( rq )
                rq.t1 = "T8" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T3"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result ) {
                    throw( TestFailed, "Expected true for typeDefinitionLessThan between the same type T8 against T3" )
                }

                undef( rq )
                rq.t1 = "T3" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T8"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( result ) {
                    throw( TestFailed, "Expected false for typeDefinitionLessThan between the same type T3 against T8" )
                }

                undef( rq )
                rq.t1 = "T9" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T2"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result ) {
                    throw( TestFailed, "Expected true for typeDefinitionLessThan between the same type T9 against T2" )
                }

                undef( rq )
                rq.t1 = "T2" 
                rq.t1.types << meta_description.input.interfaces.types
                rq.t2 = "T9"
                rq.t2.types << meta_description.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( result ) {
                    throw( TestFailed, "Expected false for typeDefinitionLessThan between the same type T2 against T9" )
                }

                // checking interfaces
                undef( rq )
                rq.i1 -> meta_description.input.interfaces
                rq.i2 -> meta_description.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result.result ) {
                    throw( TestFailed, "Expected true for interfaceDefinitionLessThan between the same interface" )
                }

                /// testing lessThan operations
                undef( rq )
                rq.filename = "private/sample_service.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description2 )
                undef( rq )
                rq.i1 -> meta_description2.input.interfaces
                rq.i2 -> meta_description.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result.result ) {
                    throw( TestFailed, "Expected true for interfaceDefinitionLessThan between TmpInterface and TmpInterface2" )
                }

                undef( rq )
                rq.i1 -> meta_description.input.interfaces
                rq.i2 -> meta_description2.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( result.result ) {
                    throw( TestFailed, "Expected false for interfaceDefinitionLessThan between TmpInterface2 and TmpInterface" )
                }
                if ( #result.errors != 1 ) {
                    throw( TestFailed, "Expected 1 error for interfaceDefinitionLessThan between TmpInterface2 and TmpInterface" )
                }
                if ( result.errors[ 0 ] != "Operation tmp4 is missing in TmpInterface" ) {
                    throw( TestFailed, "Expected 1 error message: Operation tmp4 is missing in TmpInterface. Found: " + result.errors[ 0 ] )
                }

                undef( rq )
                rq.filename = "private/sample_service4.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description3 )

                undef( rq )
                rq.i1 -> meta_description2.input.interfaces
                rq.i2 -> meta_description3.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result.result ) {
                    throw( TestFailed, "Expected true for interfaceDefinitionLessThan between " + rq.i1.name + " and " + rq.i2.name  )
                }

                undef( rq )
                rq.i1 -> meta_description3.input.interfaces
                rq.i2 -> meta_description2.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( result.result ) {
                    throw( TestFailed, "Expected false for interfaceDefinitionLessThan between TmpInterface3 and TmpInterface" )
                }
                if ( #result.errors != 2 ) {
                    throw( TestFailed, "Expected 2 errors for interfaceDefinitionLessThan between TmpInterface3 and TmpInterface" )
                }

                undef( rq )
                rq.filename = "private/sample_service5.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description4 )


                undef( rq )
                rq.t1 = "T3" 
                rq.t1.types << meta_description4.input.interfaces.types
                rq.t2 = "T11"
                rq.t2.types << meta_description4.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result ) {
                    throw( TestFailed, "Expected true for typeDefinitionLessThan between the same type T3 against T11" )
                }

                undef( rq )
                rq.t1 = "T12" 
                rq.t1.types << meta_description4.input.interfaces.types
                rq.t2 = "T13"
                rq.t2.types << meta_description4.input.interfaces.types
                typeDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result ) {
                    throw( TestFailed, "Expected true for typeDefinitionLessThan between the same type T12 against T13" )
                }


                undef( rq )
                rq.i1 -> meta_description4.input.interfaces
                rq.i2 -> meta_description3.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( !result.result ) {
                    throw( TestFailed, "Expected true for interfaceDefinitionLessThan between TmpInterface4 and TmpInterface3" )
                }


                undef( rq )
                rq.i1 -> meta_description3.input.interfaces
                rq.i2 -> meta_description4.input.interfaces
                interfaceDefinitionLessThan@MetaJolie( rq )( result )
                if ( result.result ) {
                    throw( TestFailed, "Expected false for interfaceDefinitionLessThan between TmpInterface3 and TmpInterface4" )
                }

                with ( exp ) {
                        .errors[0] = "Type of fault Fault1 of interface TmpInterface4 is not less than type of fault Fault1 of interface TmpInterface3";
                        .errors[1] = "Fault fault3 of operation tmp2 is not present in the TmpInterface3";
                        .errors[2] = "Fault Fault5 of operation tmp3 is not present in the TmpInterface3";
                        .errors[3] = "Fault Fault7 of operation tmp3 is not present in the TmpInterface3";
                        .errors[4] = "Fault Faults6 of operation tmp3 is not present in the TmpInterface3";
                        .errors[5] = "Type undefined is not less than T9";
                        .result = false
                }
                scope( compare_values ) {
                    install( ComparisonFailed => throw( TestFailed, compare_values.ComparisonFailed ) )
                    compareValuesVectorLight@MetaJolie( { .v1 << result, .v2 << exp } )()
                }

                undef( rq )
                rq.p1 -> meta_description4.input
                rq.p2 -> meta_description3.input
                portDefinitionLessThan@MetaJolie( rq )( result ) 
                if ( !result.result ) {
                    throw( TestFailed, "Expected true for portDefinitionLessThan between meta_description4.input and meta_description3.input" )
                }

                undef( rq )
                rq.p1 -> meta_description3.input
                rq.p2 -> meta_description4.input
                portDefinitionLessThan@MetaJolie( rq )( result ) 
                if ( result.result ) {
                    throw( TestFailed, "Expected false for portDefinitionLessThan between meta_description3.input and meta_description4.input" )
                }

                // messageTypeCast
                undef( rq )
                rq.filename = "private/sample_service.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description )
                rq_mtc << {
                    message << {
                        testf = "hello"
                    },
                    types << {
                        messageTypeName = "T1",
                        types << meta_description.input.interfaces.types
                    }
                }
                scope( test_type_mismatch ) {
                    install( TypeMismatch => nullProcess )
                    messageTypeCast@MetaJolie( rq_mtc )( rq_mtc_res )
                    throw( TestFailed, "Expected fault TypeMismatch for type T1" )
                }

                rq_mtc << {
                    message << {
                        fieldchoice4 = "hello"
                    },
                    types << {
                        messageTypeName = "T3",
                        types << meta_description.input.interfaces.types
                    }
                }
                scope( test_type_mismatch ) {
                    install( TypeMismatch => nullProcess )
                    messageTypeCast@MetaJolie( rq_mtc )( rq_mtc_res )
                    throw( TestFailed, "Expected fault TypeMismatch for type T3" )
                }

                rq_mtc << {
                    message << {
                        fieldChoice2 = 1,
                        fieldChoice3 << {
                            field = 10.0,
                            recursion << {
                                fieldChoice = "11.1"
                            }
                        }
                    },
                    types << {
                        messageTypeName = "T3",
                        types << meta_description.input.interfaces.types
                    }
                }
                scope( compare_values ) {
                    install( ComparisonFailed => throw( TestFailed, compare_values.ComparisonFailed ) )
                    install( TypeMismatch => throw( TestFailed, "TypeMismatch not expected here." + compare_values.TypeMismatch ) )
                    messageTypeCast@MetaJolie( rq_mtc )( rq_mtc_res )
                    compareValuesVectorLight@MetaJolie( { .v1 << rq_mtc_res.message, .v2 << "" {
                        fieldChoice2 = "1",
                        fieldChoice3 << {
                            field = "10.0",
                            recursion << {
                                fieldChoice = 11.1
                            }
                        }
                    } } )()
                }

                undef( rq_mtc )
                rq_mtc << {
                    message << {
                        fieldChoice3 << {
                            field = 10.0,
                            recursion << {
                                fieldChoice = "11.1"
                            }
                        }
                    },
                    types << {
                        messageTypeName = "T3",
                        types << meta_description.input.interfaces.types
                    }
                }
                scope( test_type_mismatch ) {
                    install( TypeMismatch => nullProcess )
                    messageTypeCast@MetaJolie( rq_mtc )( rq_mtc_res )
                    throw( TestFailed, "Expected fault TypeMismatch for type T3 in the case fieldChoice2 is missing" )
                }

                // refinedTypes
                undef( rq )
                rq.filename = "private/sample_service_refined_types.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description )
                scope( comp_values ) {
                        install( ComparisonFailed => throw( TestFailed, compare_values.ComparisonFailed ) )
                        v2 << {
                            root_type.void_type = true
                            sub_type[ 0 ] << {
                                documentation = ""
                                name = "f6"
                                type << {
                                    root_type.double_type << true {
                                        refined_type << {
                                            ranges[ 0 ] << {
                                                .min = 4.0 
                                                .max = 5.0
                                            }
                                            ranges[ 1 ] << {
                                                .min = 10.0
                                                .max = 20.0
                                            }
                                            ranges[ 2 ] << {
                                                min = 100.0
                                                max = 200.0
                                            }
                                            ranges[ 3 ] << {
                                                min = 300.0
                                                infinite = true 
                                            }
                                        }							    
                                    }
                                }
                                cardinality << {
                                    min = 1 
                                    max = 1 
                                }										
                            }
                            sub_type[ 1 ] << {
                                documentation = ""
                                name = "f7"
                                type << {
                                    root_type.string_type <<  true {
                                        refined_type << {
                                            regex = ".*@.*\\..*"
                                        }
                                    }
                                }
                                cardinality << {
                                    .min = 1
                                    .max = 1
                                }
                            }
                            sub_type[ 2 ] << {
                                documentation = ""
                                name = "f1"
                                type.root_type << {
                                    string_type << true {
                                        refined_type << {
                                            length << {
                                                min = 0
                                                max = 10
                                            }
                                        }
                                    }
                                }
                                cardinality << {
                                    min = 1
                                    max = 1
                                }
                            }
                            sub_type[ 3 ] << {
                                documentation = ""
                                name = "f2" 
                                type.root_type << {
                                    string_type << true {
                                        refined_type << {
                                            enum[ 0 ] = "hello"
                                            enum[ 1 ] = "homer"
                                            enum[ 2 ] = "simpsons"
                                        }
                                    }
                                }
                                cardinality << {
                                    min = 1
                                    max = 1
                                }
                            }
                            sub_type[ 4 ] << {
                                documentation = ""
                                name = "f3"
                                type.root_type << {
                                    string_type << true {
                                        refined_type << {
                                            length << {
                                                min = 0
                                                max = 20 
                                            }
                                        }
                                    }
                                }
                                cardinality << {
                                    min = 1
                                    max = 1
                                }
                            }
                            sub_type[ 5 ] << {
                                documentation = ""
                                name = "f4"
                                type.root_type.int_type << true {
                                    refined_type << {
                                        ranges[ 0 ] << {
                                            min = 1
                                            max = 4
                                        }
                                        ranges[ 1 ] << {
                                            min = 10
                                            max = 20
                                        }
                                        ranges[ 2 ] << {
                                            min = 100
                                            max = 200
                                        }
                                        ranges[ 3 ] << {
                                            min = 300
                                            infinite = true
                                        }
                                    }
                                }
                                cardinality << {
                                    min = 1 
                                    max = 1 
                                }
                            }
                            sub_type[ 6 ] << {
                                documentation = ""
                                name = "f5"
                                type.root_type.long_type <<  true {
                                    refined_type << {
                                        ranges[ 0 ] << {
                                            min = 3L
                                            max = 4L
                                        }
                                        ranges[ 1 ] << {
                                            min = 10L
                                            max = 20L
                                        }
                                        ranges[ 2 ] << {
                                            min = 100L
                                            max = 200L
                                        }
                                        ranges[ 3 ] << {
                                            min = 300L
                                            infinite = true
                                        }
                                    }
                                }
                                cardinality << {
                                    min = 1 
                                    max = 1 
                                }
                            }
                        }
                        compareValuesVectorLight@MetaJolie( { .v1 << meta_description.input.interfaces.types.type, .v2 << v2 })()

                }

                // check type names
                rq.filename = "private/sample_service_typesymbols.ol"
                getOutputPortMetaData@MetaJolie( rq )( meta_description )
                for( t in meta_description.output.interfaces.types ) {
                    find@StringUtils( t.name { regex = "#" } )( find_result )
                    if ( find_result > 0 ) {
                        throw( TestFailed, "Found a type with a name that containes #: " + t.name )
                    }
                }

                // check two inputports
                undef( rq )
                rq.filename = "private/sample_service7.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description )
                if ( #meta_description.input != 2 ) {
                    throw( TestFailed, "Test \"check two inputports\": Expected 2 input port, found " + #meta_description.input )
                }

                for( ip in meta_description.input ) {
                    if ( #ip.interfaces != 1 ) {
                            throw( TestFailed, "Test \"check two inputports\": Expected 1 interface for port " + ip.name + ", found " + #ip.interfaces )
                    }
                    for( i in ip.interfaces ) {
                        if ( #i.types != 2 ) {
                            throw( TestFailed, "Test \"check two inputports\": Expected 2 types for interface " + i.name + " and port " 
                            + ip.name + ", found " + #i.types  )
                        }
                    }
                }


                // check two outputports
                undef( rq )
                rq.filename = "private/sample_service7.ol"
                getOutputPortMetaData@MetaJolie( rq )( meta_description )
                if ( #meta_description.output != 2 ) {
                    throw( TestFailed, "Test \"check two outputports\": Expected 2 output port, found " + #meta_description.output )
                }

                for( o in meta_description.output ) {
                    if ( #o.interfaces != 1 ) {
                            throw( TestFailed, "Test \"check two outputports\": Expected 1 interface for port " + o.name + ", found " + #o.interfaces )
                    }
                    for( i in o.interfaces ) {
                        if ( #i.types != 2 ) {
                            throw( TestFailed, "Test \"check two outputports\": Expected 2 types for interface " + i.name + " and port " 
                            + o.name + ", found " + #i.types  )
                        }
                    }
                }

                // check extenders
                undef( rq )
                rq.filename = "private/sample_service_with_extender.ol"
                getInputPortMetaData@MetaJolie( rq )( meta_description )
                
                // check interfaces 
                if ( #meta_description.input.interfaces != 2 ) {
                    throw( TestFailed, "Test \"check extenders\": Expected 2 interfaces, found " +  #meta_description.input.interfaces )
                }
                readFile@File({
                    filename = "library/private/sample_service_extender_input_meta.json"
                    format = "json"
                })( meta_json )
                scope( comp_values ) {
                        install( ComparisonFailed => throw( TestFailed, compare_values.ComparisonFailed ) )
                        comp_req << { 
                            v1 << meta_description
                            v2 << meta_json 
                        }
                        //println@Console( valueToPrettyString@StringUtils( comp_req )  )()
                        compareValuesVectorLight@MetaJolie( comp_req )()
                }


        }
    }
}


