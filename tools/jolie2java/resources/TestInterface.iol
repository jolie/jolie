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


type VoidType: void


interface TestInterface {
RequestResponse:

  testLinkedTypeStructure( LinkedTypeStructureType )( LinkedTypeStructureVectorsType ),

  testInlineStructure( InLineStructureType )( InLineStructureVectorsType ),

  testFlatStructure( FlatStructureType )( FlatStructureVectorsType ),

  testUndefined( undefined )( double ),

  testVoidType( VoidType )( VoidType )

}
