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

type ChoiceSimpleType: string | int | double | void

type ChoiceLinkedType: LinkedTypeStructureType | int | InLineStructureType | void | FlatStructureType | FlatStructureVectorsType | string

type ChoiceInlineType: void {
  .a: string {
    .b: string {
      .c: string
    }
  }
}
| 
int
|
string {
  .d*:int {
    .e[3,5]:double {
      .f: raw
    }
  }
}
| 
void {
  .g: string 
  .m: int
}
|
string

type LinkedChoiceStructureType: void {
  a*: ChoiceSimpleType
  b*: ChoiceInlineType
  c*: ChoiceLinkedType
}

type InlineChoiceStructureType: any {
  a: int | long | double
  b: FlatStructureType | InLineStructureType | bool {?}
  c: ChoiceLinkedType | ChoiceInlineType
  d: InlineChoiceStructureType | void
}

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

type TestFaultType: void {
  .f: InLineStructureType
}

type TestFaultType2: string

type TestFaultType3: void {
  .f: void {
    .a: string
    .b: int {
      .c:bool
    }
  }
}

interface TestInterface {
OneWay:
  testOneWay( InLineStructureType ),

  testOneWay2( LinkedTypeStructureType )

RequestResponse:

  testLinkedTypeStructure( LinkedTypeStructureType )( LinkedTypeStructureType ),

  testLinkedTypeStructureVectors( LinkedTypeStructureVectorsType )( LinkedTypeStructureVectorsType ),

  testInlineStructure( InLineStructureType )( InLineStructureType ),

  testInlineStructureVectors( InLineStructureVectorsType )( InLineStructureVectorsType ),

  testFlatStructure( FlatStructureType )( FlatStructureType ),

  testFlatStructureVectors( FlatStructureVectorsType )( FlatStructureVectorsType ),

  testChoice( ChoiceSimpleType )( ChoiceSimpleType )
    throws TestFault3( string ),

  testChoiceLinkedTypes( ChoiceLinkedType )( ChoiceLinkedType ),

  testChoiceInlineTypes( ChoiceInlineType )( ChoiceInlineType ),

  testLinkedChoiceStructureType( LinkedChoiceStructureType )( LinkedChoiceStructureType ),

  testInlineChoiceStructureType( InlineChoiceStructureType )( InlineChoiceStructureType ),

  testRootValue1( RootValue1Type )( RootValue1Type )
    throws TestFault2,

  testRootValue2( RootValue2Type )( RootValue2Type )
    throws TestFault2,

  testRootValue3( RootValue3Type )( RootValue3Type ),

  testRootValue4( RootValue4Type )( RootValue4Type )
    throws TestFault2,

  testRootValue5( RootValue5Type )( RootValue5Type ),

  testRootValue6( RootValue6Type )( RootValue6Type )
    throws TestFault2,

  testRootValue7( RootValue7Type)( RootValue7Type ),

  testStringType( StringType )( StringType ),

  testVoidType( VoidType )( VoidType ),

  testDoubleType( DoubleType )( DoubleType ),

  testLongType( LongType )( LongType ),

  testRawType( RawType )( RawType ),

  testIntType( IntType )( IntType )
      throws TestFault( InLineStructureType ),

  testBoolType( BoolType )( BoolType ) 
      throws  TestFault( TestFaultType ) 
              TestFault2( TestFaultType2 )
              TestFault3( TestFaultType3 ),

  testNatives( string )( string ) throws TestFaultNatives( double ),

  testNatives2( double )( double ) throws TestFaultNatives( double ),

  testNatives3( int )( int ) throws TestFaultNatives( double ),

  testNatives4( raw )( raw ) throws TestFaultNatives( raw ),

  testVoid( void )( void ) throws TestFaultVoid( void ),

  testUndefined( undefined )( undefined ) throws TestUndefined( undefined ),

  testAny( any )( any ) throws TestAny( any )





}
