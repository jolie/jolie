package joliex.util;

import jolie.runtime.JavaService;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.java.JolieValue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import joliex.util.spec.interfaces.StringUtilsInterface;
import joliex.util.spec.types.*;

public final class NewStringUtils extends JavaService implements StringUtilsInterface {

	public Integer length( String request ) {
		return request.length();
	}

	public StringItemList sort( StringItemList request ) {
		return new StringItemList( request.item().stream().sorted().toList() );
	}

	public String replaceAll( ReplaceRequest request ) {
		return request.content().value().replaceAll( request.regex(), request.replacement() );
	}

	public String replaceFirst( ReplaceRequest request ) {
		return request.content().value().replaceFirst( request.regex(), request.replacement() );
	}

	public Boolean startsWith( StartsWithRequest request ) {
		return request.content().value().startsWith( request.prefix() );
	}

	public Boolean endsWith( EndsWithRequest request ) {
		return request.content().value().endsWith( request.suffix() );
	}

	public String join( JoinRequest request ) {
		return String.join( request.delimiter(), request.piece() );
	}

	public String trim( String request ) {
		return request.trim();
	}

	public String substring( SubStringRequest request ) {
		final String str = request.content().value();
		return str.substring( 
			request.begin(),
			request.end()
				.map( e -> Math.min( e, str.length() ) )
				.orElse( str.length() )
		);
	}

	public SplitResult split( SplitRequest request ) {
		return new SplitResult( Arrays.asList( request.content().value().split( request.regex(), request.limit().orElse( 0 ) ) ) );
	}

	public SplitResult splitByLength( SplitByLengthRequest request ) {
		final String str = request.content().value();
		return new SplitResult( IntStream.range( 0, Math.ceilDiv( str.length(), request.length() ) )
			.parallel()
			.map( i -> i * request.length() )
			.mapToObj( offset -> str.substring( offset, Math.min( offset + request.length(), str.length() ) ) )
			.toList() );
	}

	public MatchResult match( MatchRequest request ) {
		Matcher m = Pattern.compile( request.regex() ).matcher( request.content().value() );
		return m.matches()
			? MatchResult.builder()
				.contentValue( 1 )
				.group(
					IntStream.rangeClosed( 1, m.groupCount() )
						.parallel()
						.mapToObj( m::group )
						.map( s -> s == null ? "" : s )
						.toList() )
				.build()
			: MatchResult.builder().contentValue( 0 ).build();
	}

	// TODO: is it on purpose that this is the same as match in the actual StringUtils.java?
	public MatchResult find( MatchRequest request ) { return match( request ); }

	private static String padString( PadRequest request ) {
		return String.valueOf( request.chars().charAt( 0 ) )
			.repeat( Math.max( 0, request.length() - request.content().value().length() ) );
	}

	public String leftPad( PadRequest request ) {
		return padString( request ) + request.content().value();
	}

	public String rightPad( PadRequest request ) {
		return request.content().value() + padString( request );
	}

	public String valueToPrettyString( JolieValue request ) {
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer = new ValuePrettyPrinter( JolieValue.toValue( request ), writer, "root" );

		try {
			printer.run();
		} catch( IOException e ) {
		} // Should never happen

		return writer.toString();
	}

	public Integer indexOf( IndexOfRequest request ) {
		return request.content().value().indexOf( request.word() );
	}

	public String getRandomUUID() {
		return UUID.randomUUID().toString();
	}

	public String toLowerCase( String request ) {
		return request.toLowerCase();
	}

	public String toUpperCase( String request ) {
		return request.toUpperCase();
	}

	public Boolean contains( ContainsRequest request ) {
		return request.content().value().contains( request.substring() );
	}

	public String urlEncode( UrlEncodeRequest request ) throws FaultException {
		try {
			return URLEncoder.encode( request.content().value(), request.charset().orElse( "UTF-8" ) );
		} catch( UnsupportedEncodingException e ) {
			throw new FaultException( "UnsupportedEncodingException" );
		}
	}

	public String urlDecode( UrlDecodeRequest request ) throws FaultException {
		try {
			return URLDecoder.decode( request.content().value(), request.charset().orElse( "UTF-8" ) );
		} catch( UnsupportedEncodingException e ) {
			throw new FaultException( "UnsupportedEncodingException" );
		}
	}

	public String fmt( FormatRequest request ) {
		return switch( request ) {
			case FormatRequest.C1( FormatRequest.S1 option ) -> fmt_impl( option, option.content().value(), Optional.empty() );
			case FormatRequest.C2( FormatRequest.S2 option ) -> fmt_impl( option.data(), option.format(), Optional.of( option.locale() ) );
		};
	}

	private String fmt_impl( final JolieValue data, final String format, final Optional<String> locale ) {
		final StringLookup lookup = key -> {
			final String[] keyParts = key.split( ",", 2 );
			final String varName = keyParts[ 0 ];
			final Optional<String> formatting = keyParts.length > 1 ? Optional.of( keyParts[ 1 ] ) : Optional.empty();
			final JolieValue field = data.getFirstChild( varName ).orElseThrow();

			return formatting.map( f -> {
				final String pattern = "{0," + f + "}";
				final MessageFormat messageFormat = locale.map( l -> new MessageFormat( pattern, Locale.forLanguageTag( l ) ) ).orElse( new MessageFormat( pattern ) );
				return messageFormat.format( new Object[] { field.content().value() } );
			} ).orElse( String.valueOf( field.content().value() ) );
		};
		return new StringSubstitutor( lookup, "{", "}", '\\' ).replace( format );
	}
}
