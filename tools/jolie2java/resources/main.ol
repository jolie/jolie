/**
 * *************************************************************************
 * Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * Library General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA. For details about the authors of this software, see the
 * AUTHORS file.
 * *************************************************************************
 */
 include "./TestInterface.iol"

execution{ concurrent }

outputPort MyOutputPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TestInterface
}

inputPort MyPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TestInterface
}

main {
  [ testFlatStructure( request )( response ) {
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
    
  }]

  [ testOneWay( request ) ] 

  

}
