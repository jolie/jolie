package joliex.java.generate;

import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class DataManager {

	private static final Boolean TESTBOOL = true;
	private static final Integer TESTINT = 1;
	private static final Long TESTLONG = 2L;
	private static final Double TESTDOUBLE = 1.1;
	private static final String TESTSTRING = "test";
	private static final ByteArray TESTRAW = new ByteArray(
		new byte[] { (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10, (byte) 0xa2 } );

	private static final Value BOOLVALUE = Value.create( TESTBOOL );
	private static final Value INTVALUE = Value.create( TESTINT );
	private static final Value LONGVALUE = Value.create( TESTLONG );
	private static final Value DOUBLEVALUE = Value.create( TESTDOUBLE );
	private static final Value STRINGVALUE = Value.create( TESTSTRING );
	private static final Value RAWVALUE = Value.create( TESTRAW );
	private static final Value VOIDVALUE = Value.create();
	private static final Value REFINEDSTRINGTYPE = Value.create( "!" );
	private static final Value UNDEFINEDVALUE;
	private static final Value EXCEPTIONVALUE;
	private static final Value FLATSTRUCTURETYPE;
	private static final Value FLATSTRUCTUREVECTORSTYPE;
	private static final Value INLINESTRUCTURETYPE;
	private static final Value INLINESTRUCTUREVECTORSTYPE;
	private static final Value CHOICEINLINETYPE1;
	private static final Value CHOICEINLINETYPE2;
	private static final Value CHOICEINLINETYPE3;
	private static final Value INLINECHOICESTRUCTURETYPE;
	private static final Value INLINEDLINKSTRUCTURETYPE;
	private static final Value NEWTYPE;
	private static final Value LINKEDTYPESTRUCTURETYPE;
	private static final Value LINKEDTYPESTRUCTUREVECTORSTYPE;
	private static final Value LINKEDCHOICESTRUCTURETYPE;

	static {
		UNDEFINEDVALUE 					= makeUndefinedValue();
		EXCEPTIONVALUE 					= makeExceptionValue();
		FLATSTRUCTURETYPE 				= makeFlatStructureType();
		FLATSTRUCTUREVECTORSTYPE 		= makeFlatStructureVectorsType();
		INLINESTRUCTURETYPE 			= makeInlineStructureType();
		INLINESTRUCTUREVECTORSTYPE 		= makeInlineStructureVectorsType();
		CHOICEINLINETYPE1 				= makeChoiceInlineType1();
		CHOICEINLINETYPE2 				= makeChoiceInlineType2();
		CHOICEINLINETYPE3 				= makeChoiceInlineType3();
		INLINECHOICESTRUCTURETYPE 		= makeInlineChoiceStructureType();
		INLINEDLINKSTRUCTURETYPE 		= makeInlinedLinkStructureType();
		NEWTYPE 						= makeNewType();
		LINKEDTYPESTRUCTURETYPE 		= makeLinkedTypeStructureType();
		LINKEDTYPESTRUCTUREVECTORSTYPE 	= makeLinkedTypeStructureVectorsType();
		LINKEDCHOICESTRUCTURETYPE 		= makeLinkedChoiceStructureType();
	}

	public static Value getBoolValue() { return BOOLVALUE; }
	public static Value getIntValue() { return INTVALUE; }
	public static Value getLongValue() { return LONGVALUE; }
	public static Value getDoubleValue() { return DOUBLEVALUE; }
	public static Value getStringValue() { return STRINGVALUE; }
	public static Value getRawValue() { return RAWVALUE; }
	public static Value getVoidValue() { return VOIDVALUE; }
	public static Value getRefinedStringType() { return REFINEDSTRINGTYPE; }
	public static Value getUndefinedValue() { return UNDEFINEDVALUE; }
	public static Value getExceptionValue() { return EXCEPTIONVALUE; }
	public static Value getFlatStructureType() { return FLATSTRUCTURETYPE; }
	public static Value getFlatStructureVectorsType() { return FLATSTRUCTUREVECTORSTYPE; }
	public static Value getInlineStructureType() { return INLINESTRUCTURETYPE; }
	public static Value getInlineStructureVectorsType() { return INLINESTRUCTUREVECTORSTYPE; }
	public static Value getChoiceInlineType1() { return CHOICEINLINETYPE1; }
	public static Value getChoiceInlineType2() { return CHOICEINLINETYPE2; }
	public static Value getChoiceInlineType3() { return CHOICEINLINETYPE3; }
	public static Value getInlineChoiceStructureType() { return INLINECHOICESTRUCTURETYPE; }
	public static Value getInlinedLinkStructureType() { return INLINEDLINKSTRUCTURETYPE; }
	public static Value getNewType() { return NEWTYPE; }
	public static Value getLinkedTypeStructureType() { return LINKEDTYPESTRUCTURETYPE; }
	public static Value getLinkedTypeStructureVectorsType() { return LINKEDTYPESTRUCTUREVECTORSTYPE; }
	public static Value getLinkedChoiceStructureType() { return LINKEDCHOICESTRUCTURETYPE; }
	public static Value getRootValue( int index ) {
		Value returnValue = Value.create();
		Value field = returnValue.getFirstChild( "field" );
		switch( index ) {
		case 1:
			returnValue.setValue( TESTBOOL );
			field.setValue( TESTBOOL );
			break;
		case 2:
			returnValue.setValue( TESTINT );
			field.setValue( TESTINT );
			break;
		case 3:
			returnValue.setValue( TESTLONG );
			field.setValue( TESTLONG );
			break;
		case 4:
			returnValue.setValue( TESTDOUBLE );
			field.setValue( TESTDOUBLE );
			break;
		case 5:
			returnValue.setValue( TESTSTRING );
			field.setValue( TESTSTRING );
			break;
		case 6:
			returnValue.setValue( TESTRAW );
			field.setValue( TESTRAW );
			break;
		}
		return returnValue;
	}

	private static Value makeExceptionValue() {
		Value v = Value.create();
		v.setFirstChild( "zzzzzz", TESTBOOL );
		return v;
	}

	private static Value makeNewType() {
		Value testValue = Value.create();
		testValue.setFirstChild( "a", TESTSTRING );
		ValueVector b = testValue.getChildren( "b" );
		for( int i = 0; i < 100; i++ ) {
			b.get( i ).setValue( TESTINT );
			Value c = b.get( i ).getFirstChild( "c" );
			c.setValue( TESTLONG );
			c.setFirstChild( "d", TESTRAW );
			c.getChildren( "e" ).add( getFlatStructureType() );
		}
		return testValue;
	}

	private static Value makeLinkedChoiceStructureType() {
		final Value testValue = Value.create();
		ValueVector vv;
		
		vv = testValue.getChildren( "a" );
		vv.add( Value.create( TESTDOUBLE ) );
		vv.add( Value.create( TESTSTRING ) );

		vv = testValue.getChildren( "b" );
		vv.add( getChoiceInlineType2() );
		vv.add( getChoiceInlineType1() );
		vv.add( Value.create( TESTINT ) );
		vv.add( getChoiceInlineType3() );

		vv = testValue.getChildren( "c" );
		vv.add( Value.create( TESTINT ) );
		vv.add( getInlineStructureType() );
		vv.add( getFlatStructureVectorsType() );

		return testValue;
	}

	private static Value makeInlineChoiceStructureType() {
		final Value testValue = Value.create( TESTRAW );
		testValue.setFirstChild( "a", TESTLONG );
		testValue.getChildren( "b" ).add( getFlatStructureType() );
		testValue.getChildren( "c" ).add( getChoiceInlineType3() );
		
		final Value d = testValue.getFirstChild( "d" );
		d.setValue( TESTBOOL );
		d.setFirstChild( "a", TESTDOUBLE );
		d.setFirstChild( "b", TESTBOOL );
		d.getFirstChild( "c" );
		d.getFirstChild( "d" );

		return testValue;
	}

	private static Value makeInlinedLinkStructureType() {
		final Value v = Value.create();
		v.setFirstChild( "inlinedString", "World" );
		v.setFirstChild( "inlinedLink", "!" );
		v.setFirstChild( "inlinedLinkString", "Hello" );
		return v;
	}

	private static Value makeChoiceInlineType1() {
		Value testValue = Value.create();
		Value a = testValue.getFirstChild( "a" );
		a.setValue( TESTSTRING );
		Value b = a.getFirstChild( "b" );
		b.setValue( TESTSTRING );
		Value c = b.getFirstChild( "c" );
		c.setValue( TESTSTRING );
		return testValue;
	}

	private static Value makeChoiceInlineType2() {
		Value testValue = Value.create( TESTSTRING );
		ValueVector d = testValue.getChildren( "d" );
		for( int i = 0; i < 20; i++ ) {
			d.get( i ).setValue( TESTINT );
			ValueVector e = d.get( i ).getChildren( "e" );
			for( int y = 0; y < 5; y++ ) {
				e.get( y ).setValue( TESTDOUBLE );
				e.get( y ).getFirstChild( "f" ).setValue( TESTRAW );
			}
		}
		return testValue;
	}

	private static Value makeChoiceInlineType3() {
		Value testValue = Value.create();
		testValue.getFirstChild( "g" ).setValue( TESTSTRING );
		testValue.getFirstChild( "m" ).setValue( TESTINT );
		return testValue;
	}

	private static Value makeLinkedTypeStructureType() {
		Value testValue = Value.create();

		testValue.getChildren( "a" ).add( getInlineStructureType() );
		testValue.getChildren( "b" ).add( getInlineStructureVectorsType() );
		testValue.getChildren( "c" ).add( getFlatStructureType() );
		testValue.getChildren( "d" ).add( getNewType() );
		testValue.getChildren( "e" ).add( getInlinedLinkStructureType() );

		return testValue;
	}

	private static Value makeLinkedTypeStructureVectorsType() {
		Value testValue = Value.create();
		ValueVector a = testValue.getChildren( "a" );
		for( int i = 0; i < 50; i++ ) {
			a.add( getInlineStructureType() );
		}
		Value b = testValue.getFirstChild( "b" );
		ValueVector bb = b.getChildren( "bb" );
		for( int i = 0; i < 10; i++ ) {
			bb.add( getInlineStructureVectorsType() );
		}
		ValueVector c = testValue.getChildren( "c" );
		for( int i = 0; i < 7; i++ ) {
			c.add( getFlatStructureType() );
		}
		ValueVector d = testValue.getChildren( "d" );
		for( int i = 0; i < 50; i++ ) {
			d.add( getNewType() );
		}
		return testValue;
	}

	private static Value makeInlineStructureType() {
		Value testValue = Value.create();
		Value a = testValue.getFirstChild( "a" );
		a.getFirstChild( "b" ).setValue( TESTSTRING );
		a.getFirstChild( "c" ).setValue( TESTINT );
		a.getFirstChild( "f" ).setValue( TESTDOUBLE );
		Value e = a.getFirstChild( "e" );
		e.setValue( TESTSTRING );
		e.getFirstChild( "ab" ).setValue( TESTRAW );
		e.getFirstChild( "bc" ).setValue( TESTSTRING );
		Value fh = e.getFirstChild( "fh" );
		fh.setValue( TESTSTRING );
		fh.getFirstChild( "abc" ).setValue( TESTSTRING );
		fh.getFirstChild( "def" ).setValue( TESTLONG );
		Value aa = testValue.getFirstChild( "aa" );
		aa.setValue( TESTSTRING );
		aa.getFirstChild( "z" ).setValue( TESTINT );
		aa.getFirstChild( "c" ).setValue( TESTDOUBLE );
		aa.getFirstChild( "f" ).getFirstChild( "rm" ).setValue( TESTSTRING );
		return testValue;
	}

	private static Value makeInlineStructureVectorsType() {
		Value testValue = Value.create();
		ValueVector a = testValue.getChildren( "a" );
		for( int x = 0; x < 10; x++ ) {
			for( int i = 0; i < 10; i++ ) {
				a.get( x ).getNewChild( "b" ).setValue( TESTSTRING );
			}
			a.get( x ).getFirstChild( "c" ).setValue( TESTINT );
			for( int i = 0; i < 9; i++ ) {
				a.get( x ).getNewChild( "f" ).setValue( TESTDOUBLE );
			}
			ValueVector e = a.get( x ).getChildren( "e" );
			for( int i = 0; i < 8; i++ ) {
				e.get( i ).setValue( TESTSTRING );
				e.get( i ).getFirstChild( "ab" ).setValue( TESTRAW );
				for( int y = 0; y < 4; y++ ) {
					e.get( i ).getNewChild( "bc" ).setValue( TESTSTRING );
				}
				ValueVector fh = e.get( i ).getChildren( "fh" );
				for( int y = 0; y < 100; y++ ) {
					fh.get( y ).setValue( TESTSTRING );
					ValueVector abc = fh.get( y ).getChildren( "abc" );
					for( int z = 0; z < 2; z++ ) {
						abc.get( z ).setValue( TESTSTRING );
					}
					fh.get( y ).getFirstChild( "def" ).setValue( TESTLONG );
				}
			}
		}
		Value aa = testValue.getFirstChild( "aa" );
		aa.setValue( TESTSTRING );
		for( int i = 0; i < 5; i++ ) {
			aa.getNewChild( "z" ).setValue( TESTINT );
		}
		for( int i = 0; i < 3; i++ ) {
			aa.getNewChild( "c" ).setValue( TESTDOUBLE );
		}
		ValueVector f = aa.getChildren( "f" );
		for( int i = 0; i < 100; i++ ) {
			f.get( i ).getFirstChild( "rm" ).setValue( TESTSTRING );
		}
		return testValue;
	}

	private static Value makeFlatStructureType() {
		Value testValue = Value.create( TESTSTRING );
		testValue.getFirstChild( "afield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "bfield" ).setValue( TESTINT );
		testValue.getFirstChild( "cfield" ).setValue( TESTDOUBLE );
		testValue.getFirstChild( "dfield" ).setValue( TESTRAW );
		testValue.getFirstChild( "efield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "ffield" ).setValue( TESTBOOL );
		testValue.getChildren( "gfield" ).add( getUndefinedValue() );
		testValue.getFirstChild( "hfield" ).setValue( TESTLONG );
		return testValue;
	}

	private static Value makeFlatStructureVectorsType() {
		Value testValue = Value.create();
		for( int i = 0; i < 50; i++ ) {
			testValue.getNewChild( "afield" ).setValue( TESTSTRING );
		}
		// bfield is missing for testing 0 occurencies
		for( int i = 0; i < 10; i++ ) {
			testValue.getNewChild( "cfield" ).setValue( TESTDOUBLE );
		}
		for( int i = 0; i < 100; i++ ) {
			testValue.getNewChild( "dfield" ).setValue( TESTRAW );
		}
		// efield is missing for testing 0 occurencies
		for( int i = 0; i < 10; i++ ) {
			testValue.getNewChild( "ffield" ).setValue( TESTBOOL );
		}
		ValueVector gfield = testValue.getChildren( "gfield" );
		for( int i = 0; i < 4; i++ ) {
			gfield.add( getUndefinedValue() );
		}
		for( int i = 0; i < 2; i++ ) {
			testValue.getNewChild( "hfield" ).setValue( TESTLONG );
		}
		return testValue;
	}

	private static Value makeUndefinedValue() {
		Value returnValue = Value.create();
		returnValue.getFirstChild( "a" ).setValue( TESTBOOL );
		returnValue.getFirstChild( "a" ).getFirstChild( "b" ).setValue( TESTSTRING );
		returnValue.getFirstChild( "a" ).getFirstChild( "b" ).getFirstChild( "c" ).setValue( TESTDOUBLE );
		return returnValue;
	}
}
