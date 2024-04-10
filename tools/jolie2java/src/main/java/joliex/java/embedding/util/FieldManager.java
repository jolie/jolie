package joliex.java.embedding.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.TypeCheckingException;
import joliex.java.embedding.JolieNative;
import joliex.java.embedding.JolieValue;
import joliex.java.embedding.TypeValidationException;

public sealed interface FieldManager<T> {

    Integer min();
    Integer max();
    ConversionFunction<? super Value, T> valueConverter();
    Function<? super JolieValue, T> typeConverter();

    List<JolieValue> fromValueVector( ValueVector vv ) throws TypeCheckingException;
    List<JolieValue> fromJolieValues( List<JolieValue> ls );
    
    default void validateCardinality( Collection<? extends JolieValue> child ) throws TypeValidationException {
        int size = child.size();

        if ( size < min() || size > max() )
            throw new TypeValidationException( "Invalid Cardinality: the number of elements, " + size + ", is not within the range [" + min() + ":" + max() + "]." );
    }

    public static record NativeManager<U extends JolieNative<?>>( Integer min, Integer max, ConversionFunction<? super Value, U> valueConverter, Function<? super JolieValue, U> typeConverter ) implements FieldManager<U> {

        public List<JolieValue> fromValueVector( ValueVector values ) throws TypeCheckingException {
            final List<JolieValue> fieldValues = new ArrayList<>();

            for ( Value value : values )
                fieldValues.add( JolieValue.create( valueConverter().apply( value ) ) );

            return fieldValues;
        }

        public List<JolieValue> fromJolieValues( List<JolieValue> ls ) {
            return ls.parallelStream()
                .map( typeConverter()::apply )
                .map( JolieValue::create )
                .toList();
        }
    }

    public static record CustomManager<U extends JolieValue>( Integer min, Integer max, ConversionFunction<? super Value, U> valueConverter, Function<? super JolieValue, U> typeConverter ) implements FieldManager<U> {

        public List<JolieValue> fromValueVector( ValueVector values ) throws TypeCheckingException {
            final List<JolieValue> fieldValues = new ArrayList<>();
    
            for ( Value value : values )
                fieldValues.add( valueConverter().apply( value ) );
    
            return fieldValues;
        }
    
        public List<JolieValue> fromJolieValues( List<JolieValue> c ) {
            return c.parallelStream()
                .map( j -> {
                    try {
                        return typeConverter().apply( j );
                    } catch ( TypeValidationException e ) {
                        return null;
                    }
                } )
                .filter( Objects::nonNull )
                .map( JolieValue.class::cast )
                .toList();
        }
    }

    public static <U extends JolieValue> CustomManager<U> createCustom( Integer min, Integer max, ConversionFunction<Value, U> valueConverter, Function<? super JolieValue, U> typeConverter ) { return new CustomManager<>( min, max, valueConverter, typeConverter ); }
    public static <U extends JolieValue> CustomManager<U> createCustom( ConversionFunction<Value, U> valueConverter, Function<? super JolieValue, U> typeConverter ) { return createCustom( 1, 1, valueConverter, typeConverter ); }

    public static <U extends JolieNative<?>> NativeManager<U> createNative( Integer min, Integer max, ConversionFunction<Value, U> valueConverter, Function<? super JolieValue, U> typeConverter ) { return new NativeManager<>( min, max, valueConverter, typeConverter ); }
    public static <U extends JolieNative<?>> NativeManager<U> createNative( ConversionFunction<Value, U> valueConverter, Function<? super JolieValue, U> typeConverter ) { return createNative( 1, 1, valueConverter, typeConverter ); }

}