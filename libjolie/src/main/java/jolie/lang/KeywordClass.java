/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021 Vicki Mixen <vicki@mixen.dk>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang;

import java.util.Arrays;
import java.util.List;

public class KeywordClass {
	/**
	 * 
	 * @return List of keywords that can be used inside the inputPort scope
	 */
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

	/**
	 * 
	 * @return List of keywords that can be used in the outermost scope
	 */
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
			"synchronized", "scope", "install", "spawn", "over", "in", "throw", "cH", "comp", "nullProcess" );
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
		switch( scope ) {
		case "inputPort":
			return inputPortKeywords();
		case "outputPort":
			return outputPortKeywords();
		case "outer":
			return outerKeywords();
		case "main":
			return mainKeywords();
		case "interface":
			return interfaceKeywords();
		case "embedded":
			return embeddedKeywords();
		case "service":
			return serviceKeywords();
		case "execution":
			return executionKeywords();
		case "import":
			return Arrays.asList( "import" );
		default:
			return Arrays.asList();
		}
	}
}
