package joliex.java.parse.ast;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jolie.lang.NativeType;

import org.apache.commons.lang3.StringUtils;

import one.util.streamex.EntryStream;

public sealed interface JolieType {

    public static sealed interface Definition extends JolieType {

        String name();

        public static sealed interface Basic extends Definition {

            Native nativeType();
            NativeRefinement refinement();
    
            default boolean isAny() { return nativeType() == Native.ANY; }
            default boolean isVoid() { return nativeType() == Native.VOID; }
    
            public static record Inline( String name, Native nativeType, NativeRefinement refinement ) implements Basic {
    
                public boolean equals( Object obj ) { return obj != null && obj instanceof Basic other && name.equals( other.name() ); }
            }
    
            public static record Link( Inline definition ) implements Basic {
    
                public String name() { return definition.name(); }
                public Native nativeType() { return definition.nativeType(); }
                public NativeRefinement refinement() { return definition.refinement(); }
    
                public boolean equals( Object obj ) { return definition.equals( obj ); }
            }
        }
    
        public static sealed interface Structure extends Definition {
    
            public static record Field( String key, String fieldName, CompletableFuture<JolieType> typeFuture, int min, int max ) {
                
                public Optional<String> possibleName() { return Optional.ofNullable( fieldName ); }
                public JolieType type() { return typeFuture.join(); }
                public String typeName() { 
                    return switch( type() ) {
                        case Definition d -> d.name();
                        case Native n -> n.valueName();
                    };
                }
            }
    
            Native nativeType();
            NativeRefinement nativeRefinement();
            List<Field> fields();
    
            default Optional<NativeRefinement> possibleRefinement() { return Optional.ofNullable( nativeRefinement() ); }
    
            public static record Inline( String name, Native nativeType, NativeRefinement nativeRefinement, List<Field> fields ) implements Structure {
    
                public boolean equals( Object obj ) { return obj != null && obj instanceof Structure other && name.equals( other.name() ); }
            }
    
            public static record Link( Inline definition ) implements Structure {
    
                public String name() { return definition.name(); }
                public Native nativeType() { return definition.nativeType(); }
                public NativeRefinement nativeRefinement() { return definition.nativeRefinement(); }
                public List<Field> fields() { return definition.fields(); }
    
                public boolean equals( Object obj ) { return definition.equals( obj ); }
            }

            public static final class Undefined implements Structure {

                private static final Undefined instance = new Undefined();
        
                private Undefined() {}
        
                public String name() { return "JolieType"; }
                public Native nativeType() { return Native.ANY; }
                public NativeRefinement nativeRefinement() { return null; }
                public List<Field> fields() { return List.of(); }
        
                public static Undefined getInstance() { return instance; }
            }
        }
    
        public static sealed interface Choice extends Definition {
    
            public static record Option( CompletableFuture<JolieType> typeFuture ) {
    
                public JolieType type() { return typeFuture.join(); }
            }
    
            List<Option> options();
    
            default EntryStream<Integer, JolieType> numberedOptions() {
                return EntryStream.of( options() ).mapKeys( i -> ++i ).mapValues( Option::type );
            }
    
            public static record Inline( String name, List<Option> options ) implements Choice {
    
                public boolean equals( Object obj ) { return obj != null && obj instanceof Choice other && name.equals( other.name() ); }
            }
    
            public static record Link( Inline definition ) implements Choice {
    
                public String name() { return definition.name(); }
                public List<Option> options() { return definition.options(); }
    
                public boolean equals( Object obj ) { return definition.equals( obj ); }
            }
        }
    }

    public enum Native implements JolieType {
        BOOL( "Boolean", "JolieBool", "boolValue", "isBool" ),
        INT( "Integer", "JolieInt", "intValue", "isInt" ),
        LONG( "Long", "JolieLong", "longValue", "isLong" ),
        DOUBLE( "Double", "JolieDouble", "doubleValue", "isDouble" ),
        STRING( "String", "JolieString", "strValue", "isString" ),
        RAW( "ByteArray", "JolieRaw", "byteArrayValue", "isByteArray" ),
        ANY( "BasicType<?>", "BasicType<?>", "valueObject", "" ),
        VOID( "Void", "JolieVoid", "", "" );

        private static final Map<NativeType, Native> TRANSLATIONS = Map.of(
            NativeType.VOID, Native.VOID,
            NativeType.BOOL, Native.BOOL,
            NativeType.INT, Native.INT,
            NativeType.LONG, Native.LONG,
            NativeType.DOUBLE, Native.DOUBLE,
            NativeType.STRING, Native.STRING,
            NativeType.RAW, Native.RAW,
            NativeType.ANY, Native.ANY
        );

        private final String valueName;
        private final String wrapperName;
        private final String valueGetter;
        private final String valueChecker;

        private Native( String valueName, String wrapperName, String valueGetter, String valueChecker ) {
            this.valueName = valueName;
            this.wrapperName = wrapperName;
            this.valueGetter = valueGetter;
            this.valueChecker = valueChecker;
        }

        public String valueName() { return valueName; }
        public String wrapperName() { return wrapperName; }
        public String valueGetter() { return valueGetter; }
        public String valueChecker() { return valueChecker; }

        public Stream<String> valueNames() { return valueTypesOf( this ).map( Native::valueName ); }

        public String toString() {
            return StringUtils.capitalize( super.toString().toLowerCase() );
        }

        public static Native get( NativeType nativeType ) {
            return TRANSLATIONS.get( nativeType );
        }

        public static Stream<Native> valueTypesOf( Native type ) {
            return type == ANY
                ? Stream.of( values() ).filter( t -> t != ANY && t != VOID )
                : Stream.of( type );
        }
    }
}
