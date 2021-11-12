package jolie.lang;

import java.util.Arrays;
import java.util.List;

public class KeywordClass {

	public static List< String > inputPortKeywords() {
		List< String > keywords = Arrays.asList( "location", "protocol", "interfaces", "aggregates", "redirects",
			"RequestResponse", "OneWay" );
		return keywords;
	}

	public static List< String > outputPortKeywords() {
		List< String > keywords = Arrays.asList( "location", "protocol", "interfaces", "aggregates", "redirects",
			"RequestResponse", "OneWay" );
		return keywords;
	}

	public static List< String > outerKeywords() {
		List< String > keywords = Arrays.asList( "service", "interface", "from", "include", "type", "import",
			"execution", "constants", "cset", "embedded", "currier", "public", "private", "extender" );
		return keywords;
	}

	public static List< String > executionKeywords() {
		List< String > keywords = Arrays.asList( "sequential", "concurrent", "single" );
		return keywords;
	}

	public static List< String > serviceKeywords() {
		List< String > keywords =
			Arrays.asList( "execution", "init", "main", "inputPort", "outputPort", "embed", "as", "define" );
		return keywords;
	}

	public static List< String > mainKeywords() {
		List< String > keywords = Arrays.asList( "for", "while", "if", "else", "else if", "foreach", "with", "undef",
			"synchronized", "scope", "install", "spawn", "over", "in", "throw", "cH", "comp" );
		return keywords;
	}

	public static List< String > interfaceKeywords() {
		List< String > keywords = Arrays.asList( "OneWay", "oneWay", "RequestResponse", "requestResponse" );
		return keywords;
	}

	public static List< String > embeddedKeywords() {
		List< String > keywords = Arrays.asList( "Jolie", "Java", "JavaScript", "in" );
		return keywords;
	}

	public static List< String > cuorierKeywords() {
		List< String > keywords = Arrays.asList( "forward" );
		return keywords;
	}

	public static List< String > getKeywordsForScope( String scope ) {
		if( scope.equals( "inputPort" ) ) {
			return inputPortKeywords();
		} else if( scope.equals( "outputPort" ) ) {
			return outputPortKeywords();
		} else if( scope.equals( "outer" ) ) {
			return outerKeywords();
		} else if( scope.equals( "main" ) ) {
			return mainKeywords();
		} else if( scope.equals( "interface" ) ) {
			return interfaceKeywords();
		} else if( scope.equals( "embedded" ) ) {
			return embeddedKeywords();
		} else if( scope.equals( "service" ) ) {
			return serviceKeywords();
		} else if( scope.equals( "execution" ) ) {
			return executionKeywords();
		} else if( scope.equals( "import" ) ) {
			return Arrays.asList( "import" );
		} else {
			return Arrays.asList();
		}
	}
}
