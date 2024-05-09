package joliex.util.spec.types;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.ByteArray;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import jolie.runtime.embedding.java.JolieNative.*;
import jolie.runtime.embedding.java.TypedStructure;
import jolie.runtime.embedding.java.UntypedStructure;
import jolie.runtime.embedding.java.TypeValidationException;
import jolie.runtime.embedding.java.util.*;

import java.util.Arrays;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * this class is a choice type which can be described as follows:
 * <pre>
 * FormatRequest: S1 | S2
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see S1
 * @see S2
 * @see #of1(S1)
 * @see #of2(S2)
 */
public sealed interface FormatRequest extends JolieValue {
    
    Value jolieRepr();
    
    public static record C1( S1 option ) implements FormatRequest {
        
        public C1( S1 option ) { this.option = ValueManager.validated( "option", option ); }
        
        public JolieString content() { return option.content(); }
        public Map<String, List<JolieValue>> children() { return option.children(); }
        public Value jolieRepr() { return S1.toValue( option ); }
        
        public boolean equals( Object obj ) { return obj != null && obj instanceof JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public String toString() { return option.toString(); }
        
        public static C1 from( JolieValue j ) throws TypeValidationException { return new C1( S1.from( j ) ); }
        
        public static C1 fromValue( Value v ) throws TypeCheckingException { return new C1( S1.fromValue( v ) ); }
        
        public static Value toValue( C1 t ) { return t.jolieRepr(); }
    }
    
    public static record C2( S2 option ) implements FormatRequest {
        
        public C2( S2 option ) { this.option = ValueManager.validated( "option", option ); }
        
        public JolieVoid content() { return option.content(); }
        public Map<String, List<JolieValue>> children() { return option.children(); }
        public Value jolieRepr() { return S2.toValue( option ); }
        
        public boolean equals( Object obj ) { return obj != null && obj instanceof JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public String toString() { return option.toString(); }
        
        public static C2 from( JolieValue j ) throws TypeValidationException { return new C2( S2.from( j ) ); }
        
        public static C2 fromValue( Value v ) throws TypeCheckingException { return new C2( S2.fromValue( v ) ); }
        
        public static Value toValue( C2 t ) { return t.jolieRepr(); }
    }
    
    public static FormatRequest of1( S1 option ) { return new C1( option ); }
    public static FormatRequest of1( Function<S1.Builder, S1> f ) { return of1( f.apply( S1.builder() ) ); }
    
    public static FormatRequest of2( S2 option ) { return new C2( option ); }
    public static FormatRequest of2( Function<S2.Builder, S2> f ) { return of2( f.apply( S2.builder() ) ); }
    
    public static FormatRequest from( JolieValue j ) throws TypeValidationException {
        return ValueManager.choiceFrom( j, List.of( ValueManager.castFunc( C1::from ), ValueManager.castFunc( C2::from ) ) );
    }
    
    public static FormatRequest fromValue( Value v ) throws TypeCheckingException {
        return ValueManager.choiceFrom( v, List.of( ValueManager.castFunc( C1::fromValue ), ValueManager.castFunc( C2::fromValue ) ) );
    }
    
    public static Value toValue( FormatRequest t ) { return t.jolieRepr(); }
    
    
    /**
     * this class is an {@link UntypedStructure} which can be described as follows:
     * <pre>
     * 
     * contentValue: {@link String}{ ? }
     * </pre>
     * 
     * @see JolieValue
     * @see JolieNative
     * @see #builder()
     */
    public static final class S1 extends UntypedStructure<JolieString> {
        
        public S1( String contentValue, Map<String, List<JolieValue>> children ) { super( JolieNative.of( contentValue ), children ); }
        
        public String contentValue() { return content().value(); }
        
        public static Builder builder() { return new Builder(); }
        public static Builder builder( String contentValue ) { return builder().contentValue( contentValue ); }
        public static Builder builder( JolieValue from ) { return new Builder( from ); }
        
        public static StructureListBuilder<S1,Builder> listBuilder() { return new StructureListBuilder<>( S1::builder, S1::builder ); }
        public static StructureListBuilder<S1,Builder> listBuilder( SequencedCollection<? extends JolieValue> from ) {
            return new StructureListBuilder<>( from, S1::from, S1::builder, S1::builder );
        }
        
        public static S1 from( JolieValue j ) throws TypeValidationException {
            return new S1( JolieString.from( j ).value(), j.children() );
        }
        
        public static S1 fromValue( Value v ) throws TypeCheckingException { return from( JolieValue.fromValue( v ) ); }
        
        public static Value toValue( S1 t ) { return JolieValue.toValue( t ); }
        
        public static class Builder extends UntypedBuilder<Builder> {
            
            private String contentValue;
            
            private Builder() {}
            
            private Builder( JolieValue j ) {
                super( j.children() );
                
                contentValue = j.content() instanceof JolieString content ? content.value() : null;
            }
            
            protected Builder self() { return this; }
            
            public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
            
            public S1 build() { return new S1( contentValue, children ); }
        }
    }
    
    
    /**
     * this class is a {@link TypedStructure} which can be described as follows:
     * <pre>
     * data: {@link Data}
     * format: {@link String}
     * locale: {@link String}
     * </pre>
     * 
     * @see JolieValue
     * @see JolieNative
     * @see Data
     * @see #builder()
     */
    public static final class S2 extends TypedStructure {
        
        private static final Set<String> FIELD_KEYS = fieldKeys( S2.class );
        
        @JolieName("data")
        private final Data data;
        @JolieName("format")
        private final String format;
        @JolieName("locale")
        private final String locale;
        
        public S2( Data data, String format, String locale ) {
            this.data = ValueManager.validated( "data", data );
            this.format = ValueManager.validated( "format", format );
            this.locale = ValueManager.validated( "locale", locale );
        }
        
        public Data data() { return data; }
        public String format() { return format; }
        public String locale() { return locale; }
        
        public JolieVoid content() { return new JolieVoid(); }
        
        public static Builder builder() { return new Builder(); }
        public static Builder builder( JolieValue from ) { return new Builder( from ); }
        
        public static StructureListBuilder<S2,Builder> listBuilder() { return new StructureListBuilder<>( S2::builder, S2::builder ); }
        public static StructureListBuilder<S2,Builder> listBuilder( SequencedCollection<? extends JolieValue> from ) {
            return new StructureListBuilder<>( from, S2::from, S2::builder, S2::builder );
        }
        
        public static S2 from( JolieValue j ) {
            return new S2(
                ValueManager.fieldFrom( j.getFirstChild( "data" ), Data::from ),
                ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
                ValueManager.fieldFrom( j.getFirstChild( "locale" ), c -> c.content() instanceof JolieString content ? content.value() : null )
            );
        }
        
        public static S2 fromValue( Value v ) throws TypeCheckingException {
            ValueManager.requireChildren( v, FIELD_KEYS );
            return new S2(
                ValueManager.singleFieldFrom( v, "data", Data::fromValue ),
                ValueManager.singleFieldFrom( v, "format", JolieString::fieldFromValue ),
                ValueManager.singleFieldFrom( v, "locale", JolieString::fieldFromValue )
            );
        }
        
        public static Value toValue( S2 t ) {
            final Value v = Value.create();
            
            v.getFirstChild( "data" ).deepCopy( Data.toValue( t.data() ) );
            v.getFirstChild( "format" ).setValue( t.format() );
            v.getFirstChild( "locale" ).setValue( t.locale() );
            
            return v;
        }
        
        public static class Builder {
            
            private Data data;
            private String format;
            private String locale;
            
            private Builder() {}
            private Builder( JolieValue j ) {
                this.data = ValueManager.fieldFrom( j.getFirstChild( "data" ), Data::from );
                this.format = ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null );
                this.locale = ValueManager.fieldFrom( j.getFirstChild( "locale" ), c -> c.content() instanceof JolieString content ? content.value() : null );
            }
            
            public Builder data( Data data ) { this.data = data; return this; }
            public Builder data( Function<Data.Builder,Data> f ) { return data( f.apply( Data.builder() ) ); }
            public Builder format( String format ) { this.format = format; return this; }
            public Builder locale( String locale ) { this.locale = locale; return this; }
            
            public S2 build() {
                return new S2( data, format, locale );
            }
        }
        
        
        /**
         * this class is an {@link UntypedStructure} which can be described as follows:
         * <pre>
         * 
         * content: {@link JolieVoid} { ? }
         * </pre>
         * 
         * @see JolieValue
         * @see JolieNative
         * @see #builder()
         */
        public static final class Data extends UntypedStructure<JolieVoid> {
            
            public Data( Map<String, List<JolieValue>> children ) { super( new JolieVoid(), children ); }
            
            public static Builder builder() { return new Builder(); }
            public static Builder builder( JolieValue from ) { return new Builder( from ); }
            
            public static StructureListBuilder<Data,Builder> listBuilder() { return new StructureListBuilder<>( Data::builder, Data::builder ); }
            public static StructureListBuilder<Data,Builder> listBuilder( SequencedCollection<? extends JolieValue> from ) {
                return new StructureListBuilder<>( from, Data::from, Data::builder, Data::builder );
            }
            
            public static Data from( JolieValue j ) throws TypeValidationException {
                return new Data( j.children() );
            }
            
            public static Data fromValue( Value v ) throws TypeCheckingException { return from( JolieValue.fromValue( v ) ); }
            
            public static Value toValue( Data t ) { return JolieValue.toValue( t ); }
            
            public static class Builder extends UntypedBuilder<Builder> {
                
                private Builder() {}
                
                private Builder( JolieValue j ) {
                    super( j.children() );
                }
                
                protected Builder self() { return this; }
                
                public Data build() { return new Data( children ); }
            }
        }
    }
}