type NewType: void {
    .a: string
    .b*: int {
        .c: long {
            .d: raw
            .e: FlatStructureType
        }
    }
}

type FlatStructureType: string {
    .afield: string
    .bfield: int
    .cfield: double
    .dfield: raw
    .efield: any
    .ffield: bool
    .gfield: undefined
    .hfield: long
}

type FlatStructureVectorsType: void {
    .afield*: string
    .bfield?: int
    .cfield[3,10]: double
    .dfield[0,100]: raw
    .efield*: any
    .ffield*: bool
    .gfield[4,7]: undefined
    .hfield[2,2]: long
}


type InLineStructureType: void {
	.a: void {
		.b: string
		.c: int
    .f: double
    .e: string {
        .ab: raw
        .bc: string
        .fh: string {
            .abc: any
            .def: long
        }
    }
	}
	.aa: string {
		.z:int
        .c:double
        .f: void {
			.rm: string
		}
	}
}

type InLineStructureVectorsType: void {
	.a*: void {
		.b[2,10]: string
		.c: int
        .f[7,9]: double
        .e[1,8]: string {
      			.ab: raw
      			.bc[3,4]: string
      			.fh*: string {
					.abc[2,2]: any
					.def: long
				}
        }
	}
	.aa?: string {
		.z[4,5]:int
        .c[1,3]:double
        .f*: void {
			.rm: string
		}
	}
}

type LinkedTypeStructureType: void {
  .a: InLineStructureType
  .b: InLineStructureVectorsType
  .c: FlatStructureType
  .d: NewType
}

type LinkedTypeStructureVectorsType: void {
  .a*: InLineStructureType
  .b?: void {
    .bb[2,10]: InLineStructureVectorsType
  }
  .c[3,7]: FlatStructureType
  .d*: NewType
}

//type ChoiceSimpleType: string | int | double

//type ChoiceLinkedType: LinkedTypeStructureType | InLineStructureType | FlatStructureType



type RootValue1Type: string {
  .field: string
}

type RootValue2Type: int {
  .field: int
}

type RootValue3Type: double {
  .field: double
}

type RootValue4Type: raw {
  .field: raw
}

type RootValue5Type: long {
  .field: long
}

type RootValue6Type: bool {
  .field: bool
}

type RootValue7Type: any {
  .field: any
}

type StringType: string

type VoidType: void

type DoubleType: double

type LongType: long

type RawType: raw

type IntType: int

type BoolType: bool


interface TestInterface {
RequestResponse:

  testLinkedTypeStructure( LinkedTypeStructureType )( LinkedTypeStructureVectorsType ),

  testInlineStructure( InLineStructureType )( InLineStructureVectorsType ),

  testFlatStructure( FlatStructureType )( FlatStructureVectorsType ),

  //testChoice( ChoiceSimpleType )( ChoiceSimpleType ),

  //testChoiceLinkedTypes( ChoiceLinkedType )( ChoiceLinkedType ),

  testRootValue1( RootValue1Type )( RootValue2Type ),

  testRootValue2( RootValue3Type )( RootValue4Type ),

  testRootValue3( RootValue5Type )( RootValue6Type ),

  testRootValue4( RootValue7Type)( RootValue7Type ),

  testStringType( StringType )( StringType ),

  testVoidType( VoidType )( VoidType ),

  testDoubleType( DoubleType )( DoubleType ),

  testLongType( LongType )( LongType ),

  testRawType( RawType )( RawType ),

  testIntType( IntType )( IntType ),

  testBoolType( BoolType )( BoolType )

}
