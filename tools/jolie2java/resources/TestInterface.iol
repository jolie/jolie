type FlatStructureType: void {
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


/*type InLineStructureType: void {
	.a: void {
		.b: string
		.c: int
        .f: double
        .e: string {
			.ab: raw
			.bc: string
			.fh: string
		}
}*/

type VoidType: void


interface TestInterface {
RequestResponse:

  testFlatStructure( FlatStructureType )( FlatStructureVectorsType ),

  testUndefined( undefined )( double ),

  testVoidType( VoidType )( VoidType )

}
