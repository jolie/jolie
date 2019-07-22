include "./TestInterface.iol"

execution{ concurrent }

inputPort MyPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TestInterface
}

main {
  testFlatStructure( request )( response ) {
    with( response ) {
      for( i = 0, i < 10, i++ ){
          .afield[i] = "test string" + i;
          .cfield[i] = 10.0
          .efield = "string"
          .ffield = true
      }
      .bfield = 10
      for( y = 0, y <5, y++ ) {
        .gfield[y] = "ciao"
      }
      for( y = 0, y <2, y++ ) {
        .hfield[y] = long( 10 )
      }
    }
  }
}
