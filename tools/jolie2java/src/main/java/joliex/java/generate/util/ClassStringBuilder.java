package joliex.java.generate.util;

import java.util.Optional;

public class ClassStringBuilder {

    private static final int INDENTSTEP = 4;
    private static final String COMMENTBLOCKLINEPREFIX = " * ";
    private final StringBuilder builder = new StringBuilder();
    private int indentation = 0;
    private String linePrefix = "";

    public ClassStringBuilder append( String str ) { builder.append( str ); return this; }

    public ClassStringBuilder append( int i ) { return append( String.valueOf( i ) ); }

    public ClassStringBuilder append( Optional<String> o ) { o.ifPresent( this::append ); return this; }

    public ClassStringBuilder indent() {
        indentation += INDENTSTEP;
        return this;
    }

    public ClassStringBuilder dedent() {
        indentation -= INDENTSTEP;
        return this;
    }
    public ClassStringBuilder run( Runnable appender ) {
        appender.run();
        return this;
    }

    private ClassStringBuilder appendIndent( int i ) { return append( " ".repeat( i ) ); }

    public ClassStringBuilder newline() { return append( "\n" ).appendIndent( indentation ).append( linePrefix ); }

    public ClassStringBuilder newlineAppend( String str ) { 
        str.lines().forEach( l -> newline().append( l ) ); 
        return this;
    }

    public ClassStringBuilder indentedNewlineAppend( String str ) {
        str.lines().forEach( l -> newline().appendIndent( INDENTSTEP ).append( l ) ); 
        return this;
    }
    
    public ClassStringBuilder newNewlineAppend( String str ) { return newline().newlineAppend( str ); }

    public ClassStringBuilder body( Runnable bodyAppender ) {
        return append( " {" )
            .indent()
            .run( bodyAppender )
            .dedent()
            .newlineAppend( "}" );
    }

    public ClassStringBuilder commentBlock( Runnable blockAppender ) {
        newlineAppend( "/**" );
        linePrefix = COMMENTBLOCKLINEPREFIX;
        blockAppender.run();
        linePrefix = "";
        return newlineAppend( " */" );
    }

    public ClassStringBuilder codeBlock( Runnable blockAppender ) {
        return newlineAppend( "<pre>" ).run( blockAppender ).newlineAppend( "</pre>" );
    }

    public String toString() throws ClassBuilderException {
        if ( indentation != 0 )
            throw new ClassBuilderException( "The indentation of this ClassStringBuilder wasn't 0 when 'toString()' was called." );

        return builder.toString();
    }
}
