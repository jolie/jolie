package jolie.runtime.embedding.java;

import java.util.SequencedCollection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import jolie.runtime.ByteArray;
import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.java.util.AbstractListBuilder;
import jolie.runtime.embedding.java.util.UntypedBuilder;
import jolie.runtime.embedding.java.util.ValueManager;

/**
 * Interface representing the undefined type from Jolie.
 * 
 * @see JolieNative
 * @see #content()
 * @see #children()
 * @see #builder()
 * @see #of(JolieNative)
 */
public interface JolieValue extends ValueConverter {

    /**
     * Returns the wrapped root content of the data tree represented by this class.
     * 
     * @return the wrapped root content of this data tree
     * 
     * @see JolieNative
     * @see #children()
     */
    JolieNative<?> content();

    /**
     * Returns a map of the child nodes of the data tree represented by this class.
     * 
     * @return the child nodes of this data tree
     * 
     * @see Map
     * @see List
     * @see #content()
     * @see #getChild(String)
     * @see #getFirstChild(String)
     */
    Map<String, List<JolieValue>> children();

    /**
     * Returns the nodes with the specified name,
     * or {@code null} if this data tree contains no nodes with the name.
     *
     * @param name the name of the nodes that are to be returned
     * 
     * @return the nodes with the specified name, or 
     * {@code null} if this data tree contains no nodes with the name
     * 
     * @apiNote equivalent to calling {@code children().get( name )}
     * 
     * @see #children()
     * @see #getChildOrDefault(String, List)
     * @see #getFirstChild(String)
     */
    default List<JolieValue> getChild( String name ) {
        return children().get( name );
    }

    /**
     * Returns the list of nodes with the specified name, or
     * {@code defaultValue} if this data tree contains no nodes with the name.
     *
     * @param name the name of the nodes that are to be returned
     * @param defaultValue the default list of nodes
     * 
     * @return the list of nodes with the specified name, or 
     * {@code defaultValue} if this data tree contains no nodes with the name
     * 
     * @apiNote equivalent to calling {@code children().getOrDefault( name, defaultValue )}
     * 
     * @see #children()
     * @see #getChild(String)
     * @see #getFirstChild(String)
     */
    default List<JolieValue> getChildOrDefault( String name, List<JolieValue> defaultValue ) {
        return children().getOrDefault( name, defaultValue );
    }

    /**
     * Returns an {@link Optional} wrapping the first node with the specified name,
     * being empty whenever there are no nodes with the specified name.
     * 
     * @param name the name of the node that is to be returned
     * 
     * @return an {@link Optional} wrapping the first node with the specified name,
     * being empty whenever there are no nodes with the specified name
     * 
     * @see Optional
     * @see #children()
     * @see #getChild(String)
     * @see #getChildOrDefault(String, List)
     */
    default Optional<JolieValue> getFirstChild( String name ) {
        return Optional.ofNullable( getChildOrDefault( name, null ) ).map( c -> c.isEmpty() ? null : c.getFirst() );
    }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @return a new instance of the {@link Builder} for this class
     */
    public static InlineBuilder builder() { return new InlineBuilder(); }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @param content the root content of the {@link JolieValue} being built
     * @return a new instance of the {@link Builder} for this class
     * 
     * @see #builder()
     * 
     * @implSpec implemented as {@code builder().content( content )}
     */
    public static InlineBuilder builder( JolieNative<?> content ) { return builder().content( content ); }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @param contentValue the root content value of the {@link JolieValue} being built
     * @return a new instance of the {@link Builder} for this class
     * 
     * @see #builder(JolieNative)
     * @see JolieNative#of(Boolean)
     * 
     * @implSpec implemented as {@code builder().content( JolieNative.of( contentValue ) )}
     */
    public static InlineBuilder builder( Boolean contentValue ) { return builder( JolieNative.of( contentValue ) ); }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @param contentValue the root content value of the {@link JolieValue} being built
     * @return a new instance of the {@link Builder} for this class
     * 
     * @see #builder(JolieNative)
     * @see JolieNative#of(Integer)
     * 
     * @implSpec implemented as {@code builder().content( JolieNative.of( contentValue ) )}
     */
    public static InlineBuilder builder( Integer contentValue ) { return builder( JolieNative.of( contentValue ) ); }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @param contentValue the root content value of the {@link JolieValue} being built
     * @return a new instance of the {@link Builder} for this class
     * 
     * @see #builder(JolieNative)
     * @see JolieNative#of(Long)
     * 
     * @implSpec implemented as {@code builder().content( JolieNative.of( contentValue ) )}
     */
    public static InlineBuilder builder( Long contentValue ) { return builder( JolieNative.of( contentValue ) ); }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @param contentValue the root content value of the {@link JolieValue} being built
     * @return a new instance of the {@link Builder} for this class
     * 
     * @see #builder(JolieNative)
     * @see JolieNative#of(Double)
     * 
     * @implSpec implemented as {@code builder().content( JolieNative.of( contentValue ) )}
     */
    public static InlineBuilder builder( Double contentValue ) { return builder( JolieNative.of( contentValue ) ); }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @param contentValue the root content value of the {@link JolieValue} being built
     * @return a new instance of the {@link Builder} for this class
     * 
     * @see #builder(JolieNative)
     * @see JolieNative#of(String)
     * 
     * @implSpec implemented as {@code builder().content( JolieNative.of( contentValue ) )}
     */
    public static InlineBuilder builder( String contentValue ) { return builder( JolieNative.of( contentValue ) ); }

    /**
     * Returns a new instance of the {@link Builder} for this class.
     * 
     * @param contentValue the root content value of the {@link JolieValue} being built
     * @return a new instance of the {@link Builder} for this class
     * 
     * @see #builder(JolieNative)
     * @see JolieNative#of(ByteArray)
     * 
     * @implSpec implemented as {@code builder().content( JolieNative.of( contentValue ) )}
     */
    public static InlineBuilder builder( ByteArray contentValue ) { return builder( JolieNative.of( contentValue ) ); }

    /**
     * Returns an instance of the {@link Builder} for this class with the specified {@link JolieValue} as the starting point.
     * 
     * @param from the starting point for the {@link JolieValue} being built
     * @return an instance of the {@link Builder} for this class
     * 
     * @see #builder()
     * 
     * @implNote if {@code from} is not {@code null} then {@code builder( from ).build()} returns a new {@link JolieValue} that is equivalent to {@code from}, 
     * otherwise it returns an empty {@link JolieValue}
     */
    public static InlineBuilder builder( JolieValue from ) { return from != null ? new InlineBuilder( from ) : builder(); }
    
    /**
     * Returns a new instance of the {@link ListBuilder} for this class.
     * 
     * @return a new instance of the {@link ListBuilder} for this class
     */
    public static InlineListBuilder listBuilder() { return new InlineListBuilder(); }

    /**
     * Returns an instance of the {@link ListBuilder} for this class, with the specified collection as the starting point.
     * 
     * @param from the starting point for the list being built
     * @return an instance of the {@link ListBuilder} for this class
     * 
     * @see #listBuilder()
     * 
     * @implNote if {@code from} is not {@code null} then {@code listBuilder( from ).build()} returns a new {@link List} that is equivalent to {@code from}, 
     * otherwise it returns an empty {@link List}
     */
    public static InlineListBuilder listBuilder( SequencedCollection<? extends JolieValue> from ) { return from != null ? new InlineListBuilder( from ) : listBuilder(); }

    static <R> NestedBuilder<R> nestedBuilder( Function<JolieValue,R> doneFunction ) { return new NestedBuilder<>( doneFunction ); }
    static <R> NestedBuilder<R> nestedBuilder( JolieValue from, Function<JolieValue,R> doneFunction ) { return new NestedBuilder<>( from, doneFunction ); }
    static <R> NestedListBuilder<R> nestedListBuilder( Function<List<JolieValue>,R> doneFunction ) { return new NestedListBuilder<>( doneFunction ); }
    static <R> NestedListBuilder<R> nestedListBuilder( SequencedCollection<? extends JolieValue> from, Function<List<JolieValue>,R> doneFunction ) { return new NestedListBuilder<>( from, doneFunction ); }
    
    /**
     * Returns a {@link JolieValue} consisting only of the specified content, or
     * {@code null} if that is the specified content.
     * 
     * @param content the content of the returned {@link JolieValue}
     * @return a {@link JolieValue} consisting only of the specified content
     * @throws NullPointerException if {@code content} is {@code null}
     */
    public static JolieValue of( JolieNative<?> content ) { return new UntypedStructure<>( Objects.requireNonNull( content ), Map.of() ); }

    /**
     * Returns a {@link JolieValue} consisting only of the specified content value, or
     * {@code null} if that is the specified content.
     * 
     * @param contentValue the content value of the returned {@link JolieValue}
     * @return a {@link JolieValue} consisting only of the specified content value
     * 
     * @see #of(JolieNative)
     * @see JolieNative#of(Boolean)
     * 
     * @implspec implemented as {@code of( JolieNative.of( contentValue ) )}
     */
    public static JolieValue of( Boolean contentValue ) { return of( JolieNative.of( contentValue ) ); }

    /**
     * Returns a {@link JolieValue} consisting only of the specified content value, or
     * {@code null} if that is the specified content.
     * 
     * @param contentValue the content value of the returned {@link JolieValue}
     * @return a {@link JolieValue} consisting only of the specified content value
     * 
     * @see #of(JolieNative)
     * @see JolieNative#of(Integer)
     * 
     * @implspec implemented as {@code of( JolieNative.of( contentValue ) )}
     */
    public static JolieValue of( Integer contentValue ) { return of( JolieNative.of( contentValue ) ); }

    /**
     * Returns a {@link JolieValue} consisting only of the specified content value, or
     * {@code null} if that is the specified content.
     * 
     * @param contentValue the content value of the returned {@link JolieValue}
     * @return a {@link JolieValue} consisting only of the specified content value
     * 
     * @see #of(JolieNative)
     * @see JolieNative#of(Long)
     * 
     * @implspec implemented as {@code of( JolieNative.of( contentValue ) )}
     */
    public static JolieValue of( Long contentValue ) { return of( JolieNative.of( contentValue ) ); }

    /**
     * Returns a {@link JolieValue} consisting only of the specified content value, or
     * {@code null} if that is the specified content.
     * 
     * @param contentValue the content value of the returned {@link JolieValue}
     * @return a {@link JolieValue} consisting only of the specified content value
     * 
     * @see #of(JolieNative)
     * @see JolieNative#of(Double)
     * 
     * @implspec implemented as {@code of( JolieNative.of( contentValue ) )}
     */
    public static JolieValue of( Double contentValue ) { return of( JolieNative.of( contentValue ) ); }

    /**
     * Returns a {@link JolieValue} consisting only of the specified content value, or
     * {@code null} if that is the specified content.
     * 
     * @param contentValue the content value of the returned {@link JolieValue}
     * @return a {@link JolieValue} consisting only of the specified content value
     * 
     * @see #of(JolieNative)
     * @see JolieNative#of(String)
     * 
     * @implspec implemented as {@code of( JolieNative.of( contentValue ) )}
     */
    public static JolieValue of( String contentValue ) { return of( JolieNative.of( contentValue ) ); }

    /**
     * Returns a {@link JolieValue} consisting only of the specified content value, or
     * {@code null} if that is the specified content.
     * 
     * @param contentValue the content value of the returned {@link JolieValue}
     * @return a {@link JolieValue} consisting only of the specified content value
     * 
     * @see #of(JolieNative)
     * @see JolieNative#of(ByteArray)
     * 
     * @implspec implemented as {@code of( JolieNative.of( contentValue ) )}
     */
    public static JolieValue of( ByteArray contentValue ) { return of( JolieNative.of( contentValue ) ); }

    /**
     * Returns an empty {@link JolieValue}.
     * 
     * @return an empty {@link JolieValue}
     * 
     * @see #of(JolieNative)
     * @see JolieNative#of()
     * 
     * @implspec implemented as {@code of( JolieNative.of() )}
     */
    public static JolieValue of() { return of( JolieNative.of() ); }

    public static JolieValue from( JolieValue j ) { return j; }

    public static JolieValue fromValue( Value v ) {
        return new UntypedStructure<>(
            JolieNative.contentFromValue( v ), 
            ValueManager.childrenFrom( v ) );
    }

    public static Value toValue( JolieValue t ) { 
        final Value value = t.content().jolieRepr();
        t.children().forEach( (name, ls) -> {
            if ( !ls.isEmpty() ) {
                final ValueVector vv = value.getChildren( name );
                ls.forEach( e -> vv.add( toValue( e ) ) );
            }
        } );
        return value;
    }

    static abstract class Builder<B> extends UntypedBuilder<B> {

        private JolieNative<?> content;

        protected Builder() {}
        protected Builder( JolieValue j ) {
            super( j.children() );
            content = j.content();
        }

        /**
         * Sets the root content of the data tree being built to the specified content.
         * 
         * @param content content to be set as the root content
         * @return this builder
         */
        public B content( JolieNative<?> content ) { this.content = content; return self(); }

        /**
         * Sets the root content value of the data tree being built to the specified content value.
         * 
         * @param value value to be set as the root content value
         * @return this builder
         * 
         * @see #content(JolieNative)
         * @see JolieNative#of(Boolean)
         * 
         * @implSpec implemented as {@code content( JolieNative.of( value ) )}
         */
        public B content( Boolean value ) { return content( JolieNative.of( value ) ); }

        /**
         * Sets the root content value of the data tree being built to the specified content value.
         * 
         * @param value value to be set as the root content value
         * @return this builder
         * 
         * @see #content(JolieNative)
         * @see JolieNative#of(Integer)
         * 
         * @implSpec implemented as {@code content( JolieNative.of( value ) )}
         */
        public B content( Integer value ) { return content( JolieNative.of( value ) ); }

        /**
         * Sets the root content value of the data tree being built to the specified content value.
         * 
         * @param value value to be set as the root content value
         * @return this builder
         * 
         * @see #content(JolieNative)
         * @see JolieNative#of(Long)
         * 
         * @implSpec implemented as {@code content( JolieNative.of( value ) )}
         */
        public B content( Long value ) { return content( JolieNative.of( value ) ); }

        /**
         * Sets the root content value of the data tree being built to the specified content value.
         * 
         * @param value value to be set as the root content value
         * @return this builder
         * 
         * @see #content(JolieNative)
         * @see JolieNative#of(Double)
         * 
         * @implSpec implemented as {@code content( JolieNative.of( value ) )}
         */
        public B content( Double value ) { return content( JolieNative.of( value ) ); }

        /**
         * Sets the root content value of the data tree being built to the specified content value.
         * 
         * @param value value to be set as the root content value
         * @return this builder
         * 
         * @see #content(JolieNative)
         * @see JolieNative#of(String)
         * 
         * @implSpec implemented as {@code content( JolieNative.of( value ) )}
         */
        public B content( String value ) { return content( JolieNative.of( value ) ); }

        /**
         * Sets the root content value of the data tree being built to the specified content value.
         * 
         * @param value value to be set as the root content value
         * @return this builder
         * 
         * @see #content(JolieNative)
         * @see JolieNative#of(ByteArray)
         * 
         * @implSpec implemented as {@code content( JolieNative.of( value ) )}
         */
        public B content( ByteArray value ) { return content( JolieNative.of( value ) ); }

        /**
         * Replaces the root content of the data tree being built.
         * 
         * @param operator operator used to replace the root content
         * @return this builder
         * 
         * @see #content(JolieNative)
         * 
         * @implNote if {@code content} is the current content then this is equivalent to {@code content( operator.apply( content ) )}
         */
        public B content( UnaryOperator<JolieNative<?>> operator ) { return content( operator.apply( content ) ); }

        /**
         * 
         * @return the {@link JolieValue} built by this builder
         */
        protected JolieValue build() { return new UntypedStructure<>( content == null ? JolieNative.of() : content, children ); }
    }

    public static class InlineBuilder extends Builder<InlineBuilder> {

        private InlineBuilder() {}
        private InlineBuilder( JolieValue j ) { super( j ); }

        protected InlineBuilder self() { return this; }

        public JolieValue build() { return super.build(); }
    }

    public static class NestedBuilder<R> extends Builder<NestedBuilder<R>> {

        private Function<JolieValue,R> doneFunction;

        private NestedBuilder( Function<JolieValue,R> doneFunction ) { this.doneFunction = doneFunction; }
        private NestedBuilder( JolieValue j, Function<JolieValue,R> doneFunction ) { 
            super( j );
            this.doneFunction = doneFunction;
        }

        protected NestedBuilder<R> self() { return this; }

        /**
         * 
         * @return the builder that this was returned by initially
         */
        public R done() { return doneFunction.apply( build() ); }
    }
    
    static abstract class ListBuilder<B> extends AbstractListBuilder<B, JolieValue> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, JolieValue::from ); }

        /**
         * Appends the specified element to the end of the list being built, or
         * does nothing if the element is {@code null}.
         * 
         * @param contentEntry element to be appended to the list being built
         * @return this builder
         * 
         * @see AbstractListBuilder#add(JolieValue)
         * @see JolieValue#of(JolieNative)
         * 
         * @implSpec implemented as {@code add( JolieValue.of( contentEntry ) )}
         */
        public B add( JolieNative<?> contentEntry ) { return add( JolieValue.of( contentEntry ) ); }

        /**
         * Appends the specified element to the end of the list being built, or
         * does nothing if the element is {@code null}.
         * 
         * @param valueEntry element to be appended to the list being built
         * @return this builder
         * 
         * @see #add(JolieNative)
         * @see JolieNative#of(Boolean)
         * 
         * @implSpec implemented as {@code add( JolieNative.of( contentEntry ) )}
         */
        public B add( Boolean valueEntry ) { return add( JolieNative.of( valueEntry ) ); }

        /**
         * Appends the specified element to the end of the list being built, or
         * does nothing if the element is {@code null}.
         * 
         * @param valueEntry element to be appended to the list being built
         * @return this builder
         * 
         * @see #add(JolieNative)
         * @see JolieNative#of(Integer)
         * 
         * @implSpec implemented as {@code add( JolieNative.of( contentEntry ) )}
         */
        public B add( Integer valueEntry ) { return add( JolieNative.of( valueEntry ) ); }

        /**
         * Appends the specified element to the end of the list being built, or
         * does nothing if the element is {@code null}.
         * 
         * @param valueEntry element to be appended to the list being built
         * @return this builder
         * 
         * @see #add(JolieNative)
         * @see JolieNative#of(Long)
         * 
         * @implSpec implemented as {@code add( JolieNative.of( contentEntry ) )}
         */
        public B add( Long valueEntry ) { return add( JolieNative.of( valueEntry ) ); }

        /**
         * Appends the specified element to the end of the list being built, or
         * does nothing if the element is {@code null}.
         * 
         * @param valueEntry element to be appended to the list being built
         * @return this builder
         * 
         * @see #add(JolieNative)
         * @see JolieNative#of(Double)
         * 
         * @implSpec implemented as {@code add( JolieNative.of( contentEntry ) )}
         */
        public B add( Double valueEntry ) { return add( JolieNative.of( valueEntry ) ); }

        /**
         * Appends the specified element to the end of the list being built, or
         * does nothing if the element is {@code null}.
         * 
         * @param valueEntry element to be appended to the list being built
         * @return this builder
         * 
         * @see #add(JolieNative)
         * @see JolieNative#of(String)
         * 
         * @implSpec implemented as {@code add( JolieNative.of( contentEntry ) )}
         */
        public B add( String valueEntry ) { return add( JolieNative.of( valueEntry ) ); }

        /**
         * Appends the specified element to the end of the list being built, or
         * does nothing if the element is {@code null}.
         * 
         * @param valueEntry element to be appended to the list being built
         * @return this builder
         * 
         * @see #add(JolieNative)
         * @see JolieNative#of(ByteArray)
         * 
         * @implSpec implemented as {@code add( JolieNative.of( contentEntry ) )}
         */
        public B add( ByteArray valueEntry ) { return add( JolieNative.of( valueEntry ) ); }
        
        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         */
        public NestedBuilder<B> builder() { return nestedBuilder( this::add ); }

        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @param content the root content of the node being built
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @see #builder()
         * 
         * @implSpec implemented as {@code builder().content( content )}
         */
        public NestedBuilder<B> builder( JolieNative<?> content ) { return builder().content( content ); }

        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @param contentValue the root content value of the node being built
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @see #builder(JolieNative)
         * @see JolieNative#of(Boolean)
         * 
         * @implSpec implemented as {@code builder( JolieNative.of( contentValue ) )}
         */
        public NestedBuilder<B> builder( Boolean contentValue ) { return builder( JolieNative.of( contentValue ) ); }

        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @param contentValue the root content value of the node being built
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @see #builder(JolieNative)
         * @see JolieNative#of(Integer)
         * 
         * @implSpec implemented as {@code builder( JolieNative.of( contentValue ) )}
         */
        public NestedBuilder<B> builder( Integer contentValue ) { return builder( JolieNative.of( contentValue ) ); }

        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @param contentValue the root content value of the node being built
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @see #builder(JolieNative)
         * @see JolieNative#of(Long)
         * 
         * @implSpec implemented as {@code builder( JolieNative.of( contentValue ) )}
         */
        public NestedBuilder<B> builder( Long contentValue ) { return builder( JolieNative.of( contentValue ) ); }

        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @param contentValue the root content value of the node being built
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @see #builder(JolieNative)
         * @see JolieNative#of(Double)
         * 
         * @implSpec implemented as {@code builder( JolieNative.of( contentValue ) )}
         */
        public NestedBuilder<B> builder( Double contentValue ) { return builder( JolieNative.of( contentValue ) ); }

        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @param contentValue the root content value of the node being built
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @see #builder(JolieNative)
         * @see JolieNative#of(String)
         * 
         * @implSpec implemented as {@code builder( JolieNative.of( contentValue ) )}
         */
        public NestedBuilder<B> builder( String contentValue ) { return builder( JolieNative.of( contentValue ) ); }

        /**
         * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * 
         * @param contentValue the root content value of the node being built
         * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @see #builder(JolieNative)
         * @see JolieNative#of(ByteArray)
         * 
         * @implSpec implemented as {@code builder( JolieNative.of( contentValue ) )}
         */
        public NestedBuilder<B> builder( ByteArray contentValue ) { return builder( JolieNative.of( contentValue ) ); }

        /**
         * Returns a {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built.
         * The returned builder will be initialized to the specified node.
         * 
         * @param from the node that should serve as the starting point for the returned builder
         * @return a {@link JolieValue.NestedBuilder} that can be used to build the node that should be added to the list being built
         * 
         * @implNote when {@code from} is not {@code null} calling {@code builder( from ).done()} is equivalent to {@code add( from )}
         * otherwise it is equivalent to {@code builder().done()}
         */
        public NestedBuilder<B> builder( JolieValue from ) { return from != null ? nestedBuilder( from, this::add ) : builder(); }

        /**
         * Returns a {@link JolieValue.NestedBuilder} that can be used to build the replacement of the node at the specified position of the list being built.
         * The returned builder will have been initialized to the node at the specified position in this builder.
         * 
         * @param index index of the node being rebuilt
         * @return a {@link JolieValue.NestedBuilder} that can be used to build the replacement of the node at the specified position of the list being built
         */
        public NestedBuilder<B> rebuilder( int index ) { return nestedBuilder( get( index ), e -> set( index, e ) ); }

        /**
         * Returns a {@link JolieValue.NestedBuilder} that can be used to build the replacement of the node at the specified position of the list being built.
         * The returned builder will have been initialized to the node at the specified position in this builder.
         * 
         * @param index index of the node being rebuilt
         * @param contentOperator operator to be applied to the root content of the node at the specified position
         * @return a {@link JolieValue.NestedBuilder} that can be used to build the replacement of the node at the specified position of the list being built
         * 
         * @see #rebuilder(int)
         * 
         * @implNote implemented as {@code rebuilder( index ).content( contentOperator )}
         */
        public NestedBuilder<B> rebuilder( int index, UnaryOperator<JolieNative<?>> contentOperator ) { return rebuilder( index ).content( contentOperator ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        private InlineListBuilder() {}
        private InlineListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c ); }

        protected InlineListBuilder self() { return this; }

        public List<JolieValue> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<R> extends ListBuilder<NestedListBuilder<R>> {
        
        private Function<List<JolieValue>,R> doneFunction;

        private NestedListBuilder( Function<List<JolieValue>,R> doneFunction ) { this.doneFunction = doneFunction; }
        private NestedListBuilder( SequencedCollection<? extends JolieValue> c, Function<List<JolieValue>,R> doneFunction ) { 
            super( c );
            this.doneFunction = doneFunction;
        }

        protected NestedListBuilder<R> self() { return this; }

        /**
         * 
         * @return the builder that this was returned by initially
         */
        public R done() { return doneFunction.apply( build() ); }
    }
}
