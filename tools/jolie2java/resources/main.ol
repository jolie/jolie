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
  [ testOneWay( request ) ]

  [ testOneWay2( request ) ]

  [ testLinkedTypeStructure( request )( request ) ] { nullProcess }

  [ testLinkedTypeStructureVectors( request )( request ) ] { nullProcess }

  [ testInlineStructure( request )( request ) ] { nullProcess }

  [ testInlineStructureVectors( request )( request ) ] { nullProcess }

  [ testFlatStructure( request )( request ) ] { nullProcess }

  [ testFlatStructureVectors( request )( request ) ] { nullProcess }

  [ testChoice( request )( request ) ] { nullProcess }

  [ testChoiceLinkedTypes( request )( request ) ] { nullProcess }

  [ testChoiceInlineTypes( request )( request ) ] { nullProcess }

  [ testRootValue1( request )( request ) ] { nullProcess }

  [ testRootValue2( request )( request ) ] { nullProcess }

  [ testRootValue3( request )( request ) ] { nullProcess }

  [ testRootValue4( request )( request ) ] { nullProcess }

  [ testRootValue5( request )( request ) ] { nullProcess }

  [ testRootValue6( request )( request ) ] { nullProcess }

  [ testRootValue7( request )( request ) ] { nullProcess }

  [ testStringType( request )( request ) ] { nullProcess }

  [ testVoidType( request )( request ) ] { nullProcess }

  [ testDoubleType( request )( request ) ] { nullProcess }

  [ testLongType( request )( request ) ] { nullProcess }

  [ testRawType( request )( request ) ] { nullProcess }

  [ testIntType( request )( request ) ] { nullProcess }

  [ testBoolType( request )( request ) ] { nullProcess }

  [ testNatives( request )( request ) ] { nullProcess }

  [ testNatives2( request )( request ) ] { nullProcess }

  [ testNatives3( request )( request ) ] { nullProcess }
  

}
