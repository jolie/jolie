package joliex.java.generate.util;

import java.util.Arrays;
import java.util.function.Supplier;

public enum ClassPath implements Supplier<String> {
	JAVASERVICE( "jolie.runtime.JavaService" ),
	VALUE( "jolie.runtime.Value" ),
	VALUEVECTOR( "jolie.runtime.ValueVector" ),
	BYTEARRAY( "jolie.runtime.ByteArray" ),
	FAULTEXCEPTION( "jolie.runtime.FaultException" ),
	TYPECHECKINGEXCEPTION( "jolie.runtime.typing.TypeCheckingException" ),
	REQUESTRESPONSE( "jolie.runtime.embedding.RequestResponse" ),
	JOLIEVALUE( "jolie.runtime.embedding.java.JolieValue" ),
	JOLIENATIVE( "jolie.runtime.embedding.java.JolieNative" ),
	JOLIEVOID( "jolie.runtime.embedding.java.JolieNative.JolieVoid" ),
	JOLIEBOOL( "jolie.runtime.embedding.java.JolieNative.JolieBool" ),
	JOLIEINT( "jolie.runtime.embedding.java.JolieNative.JolieInt" ),
	JOLIELONG( "jolie.runtime.embedding.java.JolieNative.JolieLong" ),
	JOLIEDOUBLE( "jolie.runtime.embedding.java.JolieNative.JolieDouble" ),
	JOLIESTRING( "jolie.runtime.embedding.java.JolieNative.JolieString" ),
	JOLIERAW( "jolie.runtime.embedding.java.JolieNative.JolieRaw" ),
	TYPEDSTRUCTURE( "jolie.runtime.embedding.java.TypedStructure" ),
	UNTYPEDSTRUCTURE( "jolie.runtime.embedding.java.UntypedStructure" ),
	TYPEVALIDATIONEXCEPTION( "jolie.runtime.embedding.java.TypeValidationException" ),
	ABSTRACTLISTBUILDER( "jolie.runtime.embedding.java.util.AbstractListBuilder" ),
	CONVERSIONFUNCTION( "jolie.runtime.embedding.java.util.ConversionFunction" ),
	JOLIENAME( "jolie.runtime.embedding.java.util.JolieName" ),
	REFINEMENTVALIDATOR( "jolie.runtime.embedding.java.util.RefinementValidator" ),
	STRUCTURELISTBUILDER( "jolie.runtime.embedding.java.util.StructureListBuilder" ),
	UNTYPEDBUILDER( "jolie.runtime.embedding.java.util.UntypedBuilder" ),
	VALUEMANAGER( "jolie.runtime.embedding.java.util.ValueManager" ),
	OBJECT( "java.lang.Object" ),
	BOOLEAN( "java.lang.Boolean" ),
	INTEGER( "java.lang.Integer" ),
	LONG( "java.lang.Long" ),
	DOUBLE( "java.lang.Double" ),
	STRING( "java.lang.String" ),
	ARRAYS( "java.util.Arrays" ),
	MAP( "java.util.Map" ),
	SEQUENCEDCOLLECTION( "java.util.SequencedCollection" ),
	LIST( "java.util.List" ),
	OPTIONAL( "java.util.Optional" ),
	OBJECTS( "java.util.Objects" ),
	SET( "java.util.Set" ),
	FUNCTION( "java.util.function.Function" );

	private final String absoluteName;

	private ClassPath( String absoluteName ) {
		this.absoluteName = absoluteName;
	}

	public String get() { return absoluteName; }

	@Override
	public String toString() { return get(); }

	public String parameterized( Object... params ) {
		return Arrays.stream( params )
			.map( Object::toString )
			.reduce( (p1, p2) -> p1 + ", " + p2 )
			.map( s -> absoluteName + "<" + s + ">" )
			.orElse( absoluteName );
	}

	public static String childrenMap() {
		return MAP.parameterized( STRING, LIST.parameterized( JOLIEVALUE ) );
	}
}
