package joliex.java.embedding.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.TypeCheckingException;
import joliex.java.embedding.JolieType;
import joliex.java.embedding.StructureType;
import joliex.java.embedding.TypeValidationException;

public sealed interface FieldManager<T extends JolieType> {

    public static record Interval<T extends Number & Comparable<T>>( T min, T max ) {}

    ConversionFunction<Value, T> valueConverter();
    Function<? super StructureType, T> structureConverter();
    void validateCardinality( Collection<? extends JolieType> structures ) throws TypeValidationException;

    default List<T> fromValueVector( ValueVector values ) throws TypeCheckingException {
        final List<T> fieldValues = new ArrayList<>();

        for ( Value value : values )
            fieldValues.add( valueConverter().apply( value ) );

        return fieldValues;
    }

    default List<StructureType> fromStructures( Collection<? extends StructureType> structures ) {
        return structures.parallelStream()
            .map( s -> {
                try {
                    return JolieType.toStructure( structureConverter().apply( s ) );
                } catch ( TypeValidationException e ) {
                    return null;
                }
            } )
            .filter( s -> s != null )
            .toList();
    }

    public static record FlatField<T extends JolieType>( ConversionFunction<Value, T> valueConverter, Function<? super StructureType, T> structureConverter ) implements FieldManager<T> {
        
        public void validateCardinality( Collection<? extends JolieType> child ) throws TypeValidationException {
            if ( child.size() != 1 )
                throw new TypeValidationException( "Invalid Cardinality: there should be exactly 1 element in this field, found " + child.size() + "." );
        }
    }

    public static record VectorField<T extends JolieType>( Integer min, Integer max, ConversionFunction<Value, T> valueConverter, Function<? super StructureType, T> structureConverter ) implements FieldManager<T> {
        
        public void validateCardinality( Collection<? extends JolieType> child ) throws TypeValidationException {
            int size = child.size();

            if ( size < min() || size > max() )
                throw new TypeValidationException( "Invalid Cardinality: the number of elements, " + size + ", is not within the range [" + min() + ":" + max() + "]." );
        }
    }

    public static <T extends JolieType> FieldManager<T> create( ConversionFunction<Value, T> valueConverter, Function<? super StructureType, T> structureConverter ) { return new FlatField<>( valueConverter, structureConverter ); }
    public static <T extends JolieType> FieldManager<T> create( Integer min, Integer max, ConversionFunction<Value, T> valueConverter, Function<? super StructureType, T> structureConverter ) { return new VectorField<>( min, max, valueConverter, structureConverter ); }
}