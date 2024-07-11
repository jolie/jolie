package jolie.runtime.embedding.java;

import java.util.SequencedCollection;
import java.util.Objects;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.util.AbstractListBuilder;

/**
 * Sealed interface representing the any type from Jolie.
 *
 * @param <T> the native type of the data stored.
 *
 * @see JolieValue
 * @see #of()
 * @see #of(Boolean)
 * @see #of(Integer)
 * @see #of(Long)
 * @see #of(Double)
 * @see #of(String)
 * @see #of(ByteArray)
 *
 * @custom.apiNote Access to the data is intended to be done using enhanced switch expression like
 *                 the following:
 *
 *                 <pre>
 * public void someOperation( JolieNative request ) {
 *      switch ( request ) {
 *          case JolieNative.JolieVoid() -> {...}
 *          case JolieNative.JolieBool( Boolean r ) -> {...}
 *          case JolieNative.JolieInt( Integer r ) -> {...}
 *          case JolieNative.JolieLong( Long r ) -> {...}
 *          case JolieNative.JolieDouble( Double r ) -> {...}
 *          case JolieNative.JolieString( String r ) -> {...}
 *          case JolieNative.JolieRaw( ByteArray r ) -> {...}
 *      }
 * }
 *                 </pre>
 */
public sealed

interface JolieNative< T > extends ValueConverter {

	/**
	 * Returns the {@link Value} representation of the data wrapped by this class. This method is used
	 * internally to communicate data to the Jolie runtime.
	 *
	 * @return the {@link Value} representation of the data wrapped by this class
	 */
	Value jolieRepr();

	/**
	 * Returns the data wrapped by this class.
	 *
	 * @return the data wrapped by this class
	 */
	default T value() {
		return null;
	}

	public static record JolieVoid() implements JolieNative< Void > {

		@Override
		public Value jolieRepr() {
			return Value.create();
		}

		@Override
		public boolean equals( Object obj ) {
			return obj != null && obj instanceof JolieVoid;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public String toString() {
			return "";
		}

		public static JolieVoid from( JolieValue j ) {
			return new JolieVoid();
		}

		public static Value requireVoid( Value v ) throws TypeCheckingException {
			if( v.isDefined() )
				throw new TypeCheckingException( "The given Value was defined, but expected void." );

			return requireNoChildren( v );
		}

		public static JolieVoid fromValue( Value v ) throws TypeCheckingException {
			requireVoid( v );
			return new JolieVoid();
		}

		public static Value toValue( JolieVoid t ) {
			return t.jolieRepr();
		}
	}

	public static record JolieBool(Boolean value) implements JolieNative< Boolean > {

		public JolieBool {
			Objects.requireNonNull( value );
		}

		@Override
		public Value jolieRepr() {
			return Value.create( value );
		}

		@Override
		public boolean equals( Object obj ) {
			return obj instanceof JolieBool b && value.equals( b.value() );
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return value.toString();
		}

		public static JolieBool from( JolieValue j ) throws TypeValidationException {
			if( Objects.requireNonNull( j ).content() instanceof JolieBool content )
				return content;

			throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: "
				+ j.content().getClass().getName() + ", expected JolieBool." );
		}

		public static Boolean contentFromValue( Value v ) throws TypeCheckingException {
			if( v.valueObject() instanceof Boolean b )
				return b;

			throw new TypeCheckingException( "The given value isn't a Boolean." );
		}

		public static Boolean fieldFromValue( Value v ) throws TypeCheckingException {
			return contentFromValue( requireNoChildren( v ) );
		}

		public static JolieBool fromValue( Value v ) throws TypeCheckingException {
			return new JolieBool( fieldFromValue( v ) );
		}

		public static Value toValue( JolieBool t ) {
			return t.jolieRepr();
		}
	}

	public static record JolieInt(Integer value) implements JolieNative< Integer > {

		public JolieInt {
			Objects.requireNonNull( value );
		}

		@Override
		public Value jolieRepr() {
			return Value.create( value );
		}

		@Override
		public boolean equals( Object obj ) {
			return obj instanceof JolieInt n && value.equals( n.value() );
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return value.toString();
		}

		public static JolieInt from( JolieValue j ) throws TypeValidationException {
			if( Objects.requireNonNull( j ).content() instanceof JolieInt content )
				return content;

			throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: "
				+ j.content().getClass().getName() + ", expected JolieInt." );
		}

		public static Integer contentFromValue( Value v ) throws TypeCheckingException {
			if( v.valueObject() instanceof Integer i )
				return i;

			throw new TypeCheckingException( "The given value isn't an Integer." );
		}

		public static Integer fieldFromValue( Value v ) throws TypeCheckingException {
			return contentFromValue( requireNoChildren( v ) );
		}

		public static JolieInt fromValue( Value v ) throws TypeCheckingException {
			return new JolieInt( fieldFromValue( v ) );
		}

		public static Value toValue( JolieInt t ) {
			return t.jolieRepr();
		}
	}

	public static record JolieLong(Long value) implements JolieNative< Long > {

		public JolieLong {
			Objects.requireNonNull( value );
		}

		@Override
		public Value jolieRepr() {
			return Value.create( value );
		}

		@Override
		public boolean equals( Object obj ) {
			return obj instanceof JolieLong n && value.equals( n.value() );
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return value.toString();
		}

		public static JolieLong from( JolieValue j ) throws TypeValidationException {
			if( Objects.requireNonNull( j ).content() instanceof JolieLong content )
				return content;

			throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: "
				+ j.content().getClass().getName() + ", expected JolieLong." );
		}

		public static Long contentFromValue( Value v ) throws TypeCheckingException {
			if( v.valueObject() instanceof Long l )
				return l;

			throw new TypeCheckingException( "The given value isn't a Long." );
		}

		public static Long fieldFromValue( Value v ) throws TypeCheckingException {
			return contentFromValue( requireNoChildren( v ) );
		}

		public static JolieLong fromValue( Value v ) throws TypeCheckingException {
			return new JolieLong( fieldFromValue( v ) );
		}

		public static Value toValue( JolieLong t ) {
			return t.jolieRepr();
		}
	}

	public static record JolieDouble(Double value) implements JolieNative< Double > {

		public JolieDouble {
			Objects.requireNonNull( value );
		}

		@Override
		public Value jolieRepr() {
			return Value.create( value );
		}

		@Override
		public boolean equals( Object obj ) {
			return obj instanceof JolieDouble n && value.equals( n.value() );
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return value.toString();
		}

		public static JolieDouble from( JolieValue j ) throws TypeValidationException {
			if( Objects.requireNonNull( j ).content() instanceof JolieDouble content )
				return content;

			throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: "
				+ j.content().getClass().getName() + ", expected JolieDouble." );
		}

		public static Double contentFromValue( Value v ) throws TypeCheckingException {
			if( v.valueObject() instanceof Double d )
				return d;

			throw new TypeCheckingException( "The given value isn't a Double." );
		}

		public static Double fieldFromValue( Value v ) throws TypeCheckingException {
			return contentFromValue( requireNoChildren( v ) );
		}

		public static JolieDouble fromValue( Value v ) throws TypeCheckingException {
			return new JolieDouble( fieldFromValue( v ) );
		}

		public static Value toValue( JolieDouble t ) {
			return t.jolieRepr();
		}
	}

	public static record JolieString(String value) implements JolieNative< String > {

		public JolieString {
			Objects.requireNonNull( value );
		}

		@Override
		public Value jolieRepr() {
			return Value.create( value );
		}

		@Override
		public boolean equals( Object obj ) {
			return obj instanceof JolieString n && value.equals( n.value() );
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return value;
		}

		public static JolieString from( JolieValue j ) throws TypeValidationException {
			if( Objects.requireNonNull( j ).content() instanceof JolieString content )
				return content;

			throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: "
				+ j.content().getClass().getName() + ", expected JolieString." );
		}

		public static String contentFromValue( Value v ) throws TypeCheckingException {
			if( v.valueObject() instanceof String i )
				return i;

			throw new TypeCheckingException( "The given value isn't a String." );
		}

		public static String fieldFromValue( Value v ) throws TypeCheckingException {
			return contentFromValue( requireNoChildren( v ) );
		}

		public static JolieString fromValue( Value v ) throws TypeCheckingException {
			return new JolieString( fieldFromValue( v ) );
		}

		public static Value toValue( JolieString t ) {
			return t.jolieRepr();
		}
	}

	public static record JolieRaw(ByteArray value) implements JolieNative< ByteArray > {

		public JolieRaw {
			Objects.requireNonNull( value );
		}

		@Override
		public Value jolieRepr() {
			return Value.create( value );
		}

		@Override
		public boolean equals( Object obj ) {
			return obj instanceof JolieRaw n && value.equals( n.value() );
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return value.toString();
		}

		public static JolieRaw from( JolieValue j ) throws TypeValidationException {
			if( Objects.requireNonNull( j ).content() instanceof JolieRaw content )
				return content;

			throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: "
				+ j.content().getClass().getName() + ", expected JolieRaw." );
		}

		public static ByteArray contentFromValue( Value v ) throws TypeCheckingException {
			if( v.valueObject() instanceof ByteArray i )
				return i;

			throw new TypeCheckingException( "The given value isn't a ByteArray." );
		}

		public static ByteArray fieldFromValue( Value v ) throws TypeCheckingException {
			return contentFromValue( requireNoChildren( v ) );
		}

		public static JolieRaw fromValue( Value v ) throws TypeCheckingException {
			return new JolieRaw( fieldFromValue( v ) );
		}

		public static Value toValue( JolieRaw t ) {
			return t.jolieRepr();
		}
	}

	public static ListBuilder listBuilder() {
		return new ListBuilder();
	}

	public static ListBuilder listBuilder( SequencedCollection< ? extends JolieValue > c ) {
		return new ListBuilder( c );
	}

	/**
	 * Returns an empty {@link JolieNative}.
	 *
	 * @return an empty {@link JolieNative}
	 */
	public static JolieVoid of() {
		return new JolieVoid();
	}

	/**
	 * Returns a {@link JolieNative} wrapping the specified value.
	 *
	 * @param value value to be wrapped
	 * @return a {@link JolieNative} wrapping the specified value
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static JolieBool of( Boolean value ) {
		return new JolieBool( value );
	}

	/**
	 * Returns a {@link JolieNative} wrapping the specified value, or {@code null} if the specified
	 * value is {@code null}.
	 *
	 * @param value value to be wrapped
	 * @return a {@link JolieNative} wrapping the specified value
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static JolieInt of( Integer value ) {
		return new JolieInt( value );
	}

	/**
	 * Returns a {@link JolieNative} wrapping the specified value, or {@code null} if the specified
	 * value is {@code null}.
	 *
	 * @param value value to be wrapped
	 * @return a {@link JolieNative} wrapping the specified value
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static JolieLong of( Long value ) {
		return new JolieLong( value );
	}

	/**
	 * Returns a {@link JolieNative} wrapping the specified value, or {@code null} if the specified
	 * value is {@code null}.
	 *
	 * @param value value to be wrapped
	 * @return a {@link JolieNative} wrapping the specified value
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static JolieDouble of( Double value ) {
		return new JolieDouble( value );
	}

	/**
	 * Returns a {@link JolieNative} wrapping the specified value, or {@code null} if the specified
	 * value is {@code null}.
	 *
	 * @param value value to be wrapped
	 * @return a {@link JolieNative} wrapping the specified value
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static JolieString of( String value ) {
		return new JolieString( value );
	}

	/**
	 * Returns a {@link JolieNative} wrapping the specified value, or {@code null} if the specified
	 * value is {@code null}.
	 *
	 * @param value value to be wrapped
	 * @return a {@link JolieNative} wrapping the specified value
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static JolieRaw of( ByteArray value ) {
		return new JolieRaw( value );
	}

	/**
	 * Converts the specified {@link JolieValue} into an instance of this class.
	 *
	 * @param j the {@link JolieValue} to convert
	 * @return the {@link JolieNative} representation of the specified {@link JolieValue}
	 * @custom.implSpec returns the result of {@code j.content()}
	 */
	public static JolieNative< ? > from( JolieValue j ) {
		return j.content();
	}

	public static JolieNative< ? > contentFromValue(
		Value v ) {
		return switch( v.valueObject() ) {
		case null -> new JolieVoid();
		case Boolean n -> new JolieBool( n );
		case Integer n -> new JolieInt( n );
		case Long n -> new JolieLong( n );
		case Double n -> new JolieDouble( n );
		case String n -> new JolieString( n );
		case ByteArray n -> new JolieRaw( n );
		default ->
			throw new UnsupportedOperationException( "The provided Value had an unsupported root content type." );
		};
	}

	public static JolieNative< ? > fromValue( Value v ) throws TypeCheckingException {
		return contentFromValue( requireNoChildren( v ) );
	}

	public static Value toValue( JolieNative< ? > any ) {
		return any.jolieRepr();
	}

	private static Value requireNoChildren( Value v ) throws TypeCheckingException {
		if( v.hasChildren() )
			throw new TypeCheckingException( "The given Value had unexpected children." );

		return v;
	}

	public static class ListBuilder extends AbstractListBuilder< ListBuilder, JolieNative< ? > > {

		private ListBuilder() {}

		private ListBuilder( SequencedCollection< ? extends JolieValue > c ) {
			super( c, JolieNative::from );
		}

		@Override
		protected ListBuilder self() {
			return this;
		}

		/**
		 * Appends the specified element to the end of the list being built, or does nothing if the element
		 * is {@code null}.
		 *
		 * @param value element to be appended to the list being built
		 * @return this builder
		 *
		 * @see AbstractListBuilder#add(Object)
		 * @see JolieNative#of(Boolean)
		 *
		 * @custom.implSpec implemented as {@code add( JolieNative.of( value ) )}
		 */
		public ListBuilder add( Boolean value ) {
			return add( of( value ) );
		}

		/**
		 * Appends the specified element to the end of the list being built, or does nothing if the element
		 * is {@code null}.
		 *
		 * @param value element to be appended to the list being built
		 * @return this builder
		 *
		 * @see AbstractListBuilder#add(Object)
		 * @see JolieNative#of(Integer)
		 *
		 * @custom.implSpec implemented as {@code add( JolieNative.of( value ) )}
		 */
		public ListBuilder add( Integer value ) {
			return add( of( value ) );
		}

		/**
		 * Appends the specified element to the end of the list being built, or does nothing if the element
		 * is {@code null}.
		 *
		 * @param value element to be appended to the list being built
		 * @return this builder
		 *
		 * @see AbstractListBuilder#add(Object)
		 * @see JolieNative#of(Long)
		 *
		 * @custom.implSpec implemented as {@code add( JolieNative.of( value ) )}
		 */
		public ListBuilder add( Long value ) {
			return add( of( value ) );
		}

		/**
		 * Appends the specified element to the end of the list being built, or does nothing if the element
		 * is {@code null}.
		 *
		 * @param value element to be appended to the list being built
		 * @return this builder
		 *
		 * @see AbstractListBuilder#add(Object)
		 * @see JolieNative#of(Double)
		 *
		 * @custom.implSpec implemented as {@code add( JolieNative.of( value ) )}
		 */
		public ListBuilder add( Double value ) {
			return add( of( value ) );
		}

		/**
		 * Appends the specified element to the end of the list being built, or does nothing if the element
		 * is {@code null}.
		 *
		 * @param value element to be appended to the list being built
		 * @return this builder
		 *
		 * @see AbstractListBuilder#add(Object)
		 * @see JolieNative#of(String)
		 *
		 * @custom.implSpec implemented as {@code add( JolieNative.of( value ) )}
		 */
		public ListBuilder add( String value ) {
			return add( of( value ) );
		}

		/**
		 * Appends the specified element to the end of the list being built, or does nothing if the element
		 * is {@code null}.
		 *
		 * @param value element to be appended to the list being built
		 * @return this builder
		 *
		 * @see AbstractListBuilder#add(Object)
		 * @see JolieNative#of(ByteArray)
		 *
		 * @custom.implSpec implemented as {@code add( JolieNative.of( value ) )}
		 */
		public ListBuilder add( ByteArray value ) {
			return add( of( value ) );
		}
	}
}
