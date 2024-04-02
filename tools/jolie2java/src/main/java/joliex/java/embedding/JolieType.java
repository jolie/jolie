package joliex.java.embedding;

import jolie.runtime.Value;
import jolie.runtime.ByteArray;
import jolie.runtime.JavaService.ValueConverter;

public sealed interface JolieType extends ValueConverter permits BasicType, StructureType {
    
    Value jolieRepr();

    public static BasicType.JolieVoid create() { return BasicType.create(); }
    public static BasicType.JolieBool create( Boolean value ) { return BasicType.create( value ); }
    public static BasicType.JolieInt create( Integer value ) { return BasicType.create( value ); }
    public static BasicType.JolieLong create( Long value ) { return BasicType.create( value ); }
    public static BasicType.JolieDouble create( Double value ) { return BasicType.create( value ); }
    public static BasicType.JolieString create( String value ) { return BasicType.create( value ); }
    public static BasicType.JolieRaw create( ByteArray value ) { return BasicType.create( value ); }

    public static StructureType.InlineBuilder construct() { return StructureType.construct(); }
    public static StructureType.InlineBuilder construct( BasicType<?> root ) { return construct().root( root ); }
    public static StructureType.InlineBuilder construct( Boolean rootValue ) { return construct( create( rootValue ) ); }
    public static StructureType.InlineBuilder construct( Integer rootValue ) { return construct( create( rootValue ) ); }
    public static StructureType.InlineBuilder construct( Long rootValue ) { return construct( create( rootValue ) ); }
    public static StructureType.InlineBuilder construct( Double rootValue ) { return construct( create( rootValue ) ); }
    public static StructureType.InlineBuilder construct( String rootValue ) { return construct( create( rootValue ) ); }
    public static StructureType.InlineBuilder construct( ByteArray rootValue ) { return construct( create( rootValue ) ); }

    public static StructureType.InlineBuilder constructFrom( JolieType t ) { return StructureType.constructFrom( t ); }
    public static JolieType createFrom( JolieType t ) { return t; }

    public static StructureType toStructure( JolieType jolieType ) { 
        return switch( jolieType ) {
            case StructureType structure -> structure;
            case BasicType<?> root -> StructureType.construct( root ).build();
        };
    }

    public static Value toValue( JolieType jolieType ) { return jolieType.jolieRepr(); }
    public static JolieType fromValue( Value value ) {
        if ( value.hasChildren() )
            return StructureType.fromValue( value );

        return BasicType.fromValue( value );
    }
}
