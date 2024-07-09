package joliex.java.parse.ast;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jolie.lang.NativeType;
import joliex.java.generate.util.ClassPath;
import org.apache.commons.lang3.StringUtils;

import one.util.streamex.EntryStream;

public sealed interface JolieType {

    public static sealed interface Definition extends JolieType {

        String name();
        default boolean isLink() { return false; }

        public static sealed interface Basic extends Definition {

            Native type();
            NativeRefinement refinement();
    
            default boolean isAny() { return type() == Native.ANY; }
            default boolean isVoid() { return type() == Native.VOID; }
    
            public static record Inline( String name, Native type, NativeRefinement refinement ) implements Basic {
    
                public boolean equals( Object obj ) { return obj != null && obj instanceof Basic other && name.equals( other.name() ); }
                public int hashCode() { return name.hashCode(); }
            }
    
            public static record Link( Inline definition ) implements Basic {
    
                public String name() { return definition.name(); }
                public Native type() { return definition.type(); }
                public NativeRefinement refinement() { return definition.refinement(); }
                
                public boolean isLink() { return true; }
    
                public boolean equals( Object obj ) { return definition.equals( obj ); }
                public int hashCode() { return definition.hashCode(); }
            }
        }
    
        public static sealed interface Structure extends Definition {
    
            public static record Field( String jolieName, String javaName, int min, int max, JolieType type ) {}
    
            Native contentType();
            NativeRefinement nativeRefinement();
            List<Field> fields();
            default boolean hasBuilder() { return true; }
            default Optional<NativeRefinement> possibleRefinement() { return Optional.ofNullable( nativeRefinement() ); }
    
            public static sealed interface Inline extends Structure {

                public static record Typed( String name, Native contentType, NativeRefinement nativeRefinement, CompletableFuture<List<Field>> fieldsFuture, boolean hasBuilder ) implements Inline {
                    
                    public List<Field> fields() { return fieldsFuture.join(); }

                    public boolean equals( Object obj ) { return obj != null && obj instanceof Structure other && name.equals( other.name() ); }
                    public int hashCode() { return name.hashCode(); }
                }

                public static record Untyped( String name, Native contentType, NativeRefinement nativeRefinement, boolean hasBuilder ) implements Inline {
                    
                    public List<Field> fields() { return List.of(); }

                    public boolean equals( Object obj ) { return obj != null && obj instanceof Structure other && name.equals( other.name() ); }
                    public int hashCode() { return name.hashCode(); }
                }
            }
    
            public static record Link( Inline definition ) implements Structure {
    
                public String name() { return definition.name(); }
                public Native contentType() { return definition.contentType(); }
                public NativeRefinement nativeRefinement() { return definition.nativeRefinement(); }
                public List<Field> fields() { return definition.fields(); }
                public boolean hasBuilder() { return definition.hasBuilder(); }
                
                public boolean isLink() { return true; }
    
                public boolean equals( Object obj ) { return definition.equals( obj ); }
                public int hashCode() { return definition.hashCode(); }
            }

            public static final class Undefined implements Structure {

                private static final Undefined INSTANCE = new Undefined();
        
                private Undefined() {}
        
                public String name() { return ClassPath.JOLIEVALUE.get(); }
                public Native contentType() { return Native.ANY; }
                public NativeRefinement nativeRefinement() { return null; }
                public List<Field> fields() { return List.of(); }
        
                public static Undefined getInstance() { return INSTANCE; }
            }
        }
    
        public static sealed interface Choice extends Definition {
    
            List<JolieType> options();
            boolean hasBuilder();
            default EntryStream<Integer, JolieType> numberedOptions() {
                return EntryStream.of( options() ).mapKeys( i -> ++i );
            }
    
            public static record Inline( String name, CompletableFuture<List<JolieType>> optionsFuture, boolean hasBuilder ) implements Choice {
                
                public List<JolieType> options() { return optionsFuture.join(); }
                public boolean equals( Object obj ) { return obj != null && obj instanceof Choice other && name.equals( other.name() ); }
                public int hashCode() { return name.hashCode(); } 
            }
    
            public static record Link( Inline definition ) implements Choice {
    
                public String name() { return definition.name(); }
                public List<JolieType> options() { return definition.options(); }
                public boolean hasBuilder() { return definition.hasBuilder(); }
                
                public boolean isLink() { return true; }
    
                public boolean equals( Object obj ) { return definition.equals( obj ); }
                public int hashCode() { return definition.hashCode(); }
            }
        }
    }

    public enum Native implements JolieType {
        BOOL(
            ClassPath.BOOLEAN, ClassPath.JOLIEBOOL, 
            "boolValue", "isBool" ),
        INT(
            ClassPath.INTEGER, ClassPath.JOLIEINT,
            "intValue", "isInt" ),
        LONG(
            ClassPath.LONG, ClassPath.JOLIELONG, 
            "longValue", "isLong" ),
        DOUBLE(
            ClassPath.DOUBLE, ClassPath.JOLIEDOUBLE,
            "doubleValue", "isDouble" ),
        STRING(
            ClassPath.STRING, ClassPath.JOLIESTRING, 
            "strValue", "isString" ),
        RAW(
            ClassPath.BYTEARRAY, ClassPath.JOLIERAW, 
            "byteArrayValue", "isByteArray" ),
        ANY(
            ClassPath.JOLIENATIVE, ClassPath.JOLIENATIVE,
            "valueObject", "" ),
        VOID(
            null, ClassPath.JOLIEVOID, 
            "", "" );

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

        private final ClassPath nativeClass;
        private final ClassPath wrapperClass;
        private final String valueGetter;
        private final String valueChecker;

        private Native( ClassPath nativeClass, ClassPath wrapperClass, String valueGetter, String valueChecker ) {
            this.nativeClass = nativeClass;
            this.wrapperClass = wrapperClass;
            this.valueGetter = valueGetter;
            this.valueChecker = valueChecker;
        }

        public ClassPath nativeClass() { return nativeClass; }
        public ClassPath wrapperClass() { return wrapperClass; }
        public String valueGetter() { return valueGetter; }
        public String valueChecker() { return valueChecker; }

        public String nativeType() { return this == ANY ? nativeClass.parameterized( "?" ) : nativeClass.get(); }
        public String wrapperType() { return this == ANY ? wrapperClass.parameterized( "?" ) : wrapperClass.get(); }

        public Stream<ClassPath> nativeClasses() { return nativeClassesOf( this ).map( Native::nativeClass ); }

        public String toString() {
            return StringUtils.capitalize( super.toString().toLowerCase() );
        }

        public static Native get( NativeType nativeType ) {
            return TRANSLATIONS.get( nativeType );
        }

        public static Stream<Native> nativeClassesOf( Native type ) {
            return type == ANY
                ? Stream.of( values() ).filter( t -> t != ANY && t != VOID )
                : Stream.of( type );
        }
    }
}
