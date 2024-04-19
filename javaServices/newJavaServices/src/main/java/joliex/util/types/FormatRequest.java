package joliex.util.types;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.ByteArray;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import jolie.runtime.embedding.java.JolieNative.*;
import jolie.runtime.embedding.java.ImmutableStructure;
import jolie.runtime.embedding.java.TypeValidationException;
import jolie.runtime.embedding.java.util.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * this class is a choice type which can be described as follows:
 * <pre>
 * FormatRequest: S1 | S2
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public sealed interface FormatRequest extends JolieValue {
    
    Value jolieRepr();
    
    public static record C1( S1 option ) implements FormatRequest {
        
        public C1( S1 option ) { this.option = Objects.requireNonNull( option ); }
        
        public JolieString content() { return option.content(); }
        public Map<String, List<JolieValue>> children() { return option.children(); }
        public Value jolieRepr() { return S1.toValue( option ); }
        
        public boolean equals( Object obj ) { return obj instanceof C1 o && option.equals( o.option() ); }
        public int hashCode() { return option.hashCode(); }
        public String toString() { return option.toString(); }
        
        public static C1 createFrom( JolieValue j ) throws TypeValidationException { return new C1( S1.createFrom( j ) ); }
        
        public static C1 fromValue( Value v ) throws TypeCheckingException { return new C1( S1.fromValue( v ) ); }
        
        public static Value toValue( C1 t ) { return t.jolieRepr(); }
    }
    
    public static record C2( S2 option ) implements FormatRequest {
        
        public C2( S2 option ) { this.option = Objects.requireNonNull( option ); }
        
        public JolieVoid content() { return option.content(); }
        public Map<String, List<JolieValue>> children() { return option.children(); }
        public Value jolieRepr() { return S2.toValue( option ); }
        
        public boolean equals( Object obj ) { return obj instanceof C2 o && option.equals( o.option() ); }
        public int hashCode() { return option.hashCode(); }
        public String toString() { return option.toString(); }
        
        public static C2 createFrom( JolieValue j ) throws TypeValidationException { return new C2( S2.createFrom( j ) ); }
        
        public static C2 fromValue( Value v ) throws TypeCheckingException { return new C2( S2.fromValue( v ) ); }
        
        public static Value toValue( C2 t ) { return t.jolieRepr(); }
    }
    
    public static FormatRequest create1( S1 option ) { return new C1( option ); }
    public static FormatRequest create1( Function<S1.Builder, S1> b ) { return create1( b.apply( S1.construct() ) ); }
    
    public static FormatRequest create2( S2 option ) { return new C2( option ); }
    public static FormatRequest create2( Function<S2.Builder, S2> b ) { return create2( b.apply( S2.construct() ) ); }
    
    public static FormatRequest createFrom( JolieValue j ) throws TypeValidationException {
        return ValueManager.choiceFrom( j, List.of( ValueManager.castFunc( C1::createFrom ), ValueManager.castFunc( C2::createFrom ) ) );
    }
    
    public static FormatRequest fromValue( Value v ) throws TypeCheckingException {
        return ValueManager.choiceFrom( v, List.of( ValueManager.castFunc( C1::fromValue ), ValueManager.castFunc( C2::fromValue ) ) );
    }
    
    public static Value toValue( FormatRequest t ) { return t.jolieRepr(); }
    
    
    /**
     * this class is an {@link ImmutableStructure} which can be described as follows:
     * <pre>
     * 
     * contentValue: {@link String}{ ? }
     * </pre>
     * 
     * @see JolieValue
     * @see JolieNative
     * @see #construct()
     */
    public static final class S1 extends ImmutableStructure<JolieString> {
        
        public S1( String contentValue, Map<String, List<JolieValue>> children ) { super( JolieNative.create( contentValue ), children ); }
        
        public String contentValue() { return content().value(); }
        
        public static Builder construct() { return new Builder(); }
        public static ListBuilder constructList() { return new ListBuilder(); }
        public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
        public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
        
        public static Builder construct( String contentValue ) { return construct().contentValue( contentValue ); }
        
        public static S1 createFrom( JolieValue j ) throws TypeValidationException {
            return new S1( JolieString.createFrom( j ).value(), j.children() );
        }
        
        public static S1 fromValue( Value v ) throws TypeCheckingException { return createFrom( JolieValue.fromValue( v ) ); }
        
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
        
        public static class ListBuilder extends AbstractListBuilder<ListBuilder, S1> {
            
            private ListBuilder() {}
            private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, S1::createFrom ); }
            
            protected ListBuilder self() { return this; }
            
            public ListBuilder add( Function<Builder, S1> b ) { return add( b.apply( construct() ) ); }
            public ListBuilder set( int index, Function<Builder, S1> b ) { return set( index, b.apply( construct() ) ); }
            public ListBuilder reconstruct( int index, Function<Builder, S1> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
        }
    }
    
    
    /**
     * this class is an {@link JolieValue} which can be described as follows:
     * <pre>
     * data: {@link Data}
     * format: {@link String}
     * locale: {@link String}
     * </pre>
     * 
     * @see JolieValue
     * @see JolieNative
     * @see Data
     * @see #construct()
     */
    public static final class S2 implements JolieValue {
        
        private static final Set<String> FIELD_KEYS = Set.of( "data", "format", "locale" );
        
        private final Data data;
        private final String format;
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
        public Map<String, List<JolieValue>> children() {
            return Map.of(
                "data", List.<JolieValue>of( data ),
                "format", List.of( JolieValue.create( format ) ),
                "locale", List.of( JolieValue.create( locale ) )
            );
        }
        
        public static Builder construct() { return new Builder(); }
        public static ListBuilder constructList() { return new ListBuilder(); }
        public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
        public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
        
        public static S2 createFrom( JolieValue j ) {
            return new S2(
                ValueManager.fieldFrom( j.getFirstChild( "data" ), Data::createFrom ),
                ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
                ValueManager.fieldFrom( j.getFirstChild( "locale" ), c -> c.content() instanceof JolieString content ? content.value() : null )
            );
        }
        
        public static S2 fromValue( Value v ) throws TypeCheckingException {
            ValueManager.requireChildren( v, FIELD_KEYS );
            return new S2(
                ValueManager.fieldFrom( v.firstChildOrDefault( "data", Function.identity(), null ), Data::fromValue ),
                ValueManager.fieldFrom( v.firstChildOrDefault( "format", Function.identity(), null ), JolieString::fieldFromValue ),
                ValueManager.fieldFrom( v.firstChildOrDefault( "locale", Function.identity(), null ), JolieString::fieldFromValue )
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
                this.data = ValueManager.fieldFrom( j.getFirstChild( "data" ), Data::createFrom );
                this.format = ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null );
                this.locale = ValueManager.fieldFrom( j.getFirstChild( "locale" ), c -> c.content() instanceof JolieString content ? content.value() : null );
            }
            
            public Builder data( Data data ) { this.data = data; return this; }
            public Builder data( Function<Data.Builder, Data> b ){ return data( b.apply( Data.construct() ) ); }
            public Builder format( String format ) { this.format = format; return this; }
            public Builder locale( String locale ) { this.locale = locale; return this; }
            
            public S2 build() {
                return new S2( data, format, locale );
            }
        }
        
        public static class ListBuilder extends AbstractListBuilder<ListBuilder, S2> {
            
            private ListBuilder() {}
            private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, S2::createFrom ); }
            
            protected ListBuilder self() { return this; }
            
            public ListBuilder add( Function<Builder, S2> b ) { return add( b.apply( construct() ) ); }
            public ListBuilder set( int index, Function<Builder, S2> b ) { return set( index, b.apply( construct() ) ); }
            public ListBuilder reconstruct( int index, Function<Builder, S2> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
        }
        
        
        /**
         * this class is an {@link ImmutableStructure} which can be described as follows:
         * <pre>
         * 
         * content: {@link JolieVoid} { ? }
         * </pre>
         * 
         * @see JolieValue
         * @see JolieNative
         * @see #construct()
         */
        public static final class Data extends ImmutableStructure<JolieVoid> {
            
            public Data( Map<String, List<JolieValue>> children ) { super( new JolieVoid(), children ); }
            
            public static Builder construct() { return new Builder(); }
            public static ListBuilder constructList() { return new ListBuilder(); }
            public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
            public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
            
            public static Data createFrom( JolieValue j ) throws TypeValidationException {
                return new Data( j.children() );
            }
            
            public static Data fromValue( Value v ) throws TypeCheckingException { return createFrom( JolieValue.fromValue( v ) ); }
            
            public static Value toValue( Data t ) { return JolieValue.toValue( t ); }
            
            public static class Builder extends UntypedBuilder<Builder> {
                
                private Builder() {}
                
                private Builder( JolieValue j ) {
                    super( j.children() );
                }
                
                protected Builder self() { return this; }
                
                public Data build() { return new Data( children ); }
            }
            
            public static class ListBuilder extends AbstractListBuilder<ListBuilder, Data> {
                
                private ListBuilder() {}
                private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, Data::createFrom ); }
                
                protected ListBuilder self() { return this; }
                
                public ListBuilder add( Function<Builder, Data> b ) { return add( b.apply( construct() ) ); }
                public ListBuilder set( int index, Function<Builder, Data> b ) { return set( index, b.apply( construct() ) ); }
                public ListBuilder reconstruct( int index, Function<Builder, Data> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
            }
        }
    }
}