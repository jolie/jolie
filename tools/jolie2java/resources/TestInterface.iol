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

type VoidType: void


interface TestInterface {
RequestResponse:

  testFlatStructure( FlatStructureType )( FlatStructureType ),

  testUndefined( undefined )( double ),

  testVoidType( VoidType )( VoidType )

}
