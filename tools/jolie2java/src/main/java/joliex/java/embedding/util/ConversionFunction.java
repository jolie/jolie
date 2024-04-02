package joliex.java.embedding.util;

import jolie.runtime.typing.TypeCheckingException;

@FunctionalInterface
public interface ConversionFunction<T,U> {
    U apply( T t ) throws TypeCheckingException;
}