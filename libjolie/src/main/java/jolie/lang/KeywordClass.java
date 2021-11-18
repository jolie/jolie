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

import java.util.List;

public class KeywordClass {

	// Can also be used for outputPort
	public static final String INPUTPORT_LOCATION = "location";
	public static final String INNPUTPORT_PROTOCOL = "protocol";
	public static final String INPUTPORT_INTERFACES = "interfaces";
	public static final String INPUTPORT_AGGREGATES = "aggregates";
	public static final String INPUTPORT_REDIRECTS = "redirects";
	public static final String REQUESTRESPONSE = "requestResponse";
	public static final String ONEWAY = "oneWay";

	// Outermost scope
	public static final String SERVICE = "service";
	public static final String INTERFACE = "interface";
	public static final String FROM = "from";
	public static final String IMPORT = "import";
	public static final String INCLUDE = "include";
	public static final String TYPE = "type";
	public static final String EXECUTION = "execution"; // can also be used inside service
	public static final String CONSTANTS = "constants";
	public static final String CSET = "cset";
	public static final String EMBEDDED = "embedded";
	public static final String CURRIER = "currier";
	public static final String PUBLIC = "public";
	public static final String PRIVATE = "private";
	public static final String EXTENDER = "extender";

	// Execution modalities
	public static final String SEQUENTIAL = "sequential";
	public static final String CONCURRENT = "concurrent";
	public static final String SINGLE = "single";

	// Service scope
	public static final String INIT = "init";
	public static final String MAIN = "main";
	public static final String INPUTPORT = "inputPort";
	public static final String OUTPUTPORT = "outputPort";
	public static final String EMBED = "embed";
	public static final String EMBED_AS = "as";
	public static final String DEFINE = "define";

	// Main and other inner scopes
	public static final String FOR = "for";
	public static final String WHILE = "while";
	public static final String IF = "if";
	public static final String ELSE = "else";
	public static final String ELSE_IF = "else if";
	public static final String FOREACH = "foreach";
	public static final String WITH = "with";
	public static final String UNDEF = "undef";
	public static final String SYNCHRONIZED = "synchronized";
	public static final String SCOPE = "scope";
	public static final String INSTALL = "install";
	public static final String SPAWN = "spawn";
	public static final String OVER = "over";
	public static final String IN = "in";
	public static final String THROW = "throw";
	public static final String CH = "cH";
	public static final String COMP = "comp";
	public static final String NULLPROCESS = "nullProcess";

	// Languages that can be embedded
	public static final String JOLIE = "Jolie";
	public static final String JAVA = "Java";
	public static final String JAVASCRIPTS = "JavaScript";

	// Currier scope
	public static final String FORWARD = "forward";


	private static final List< String > INPUTPORTKEYWORDS =
		List.of( "location", "protocol", "interfaces", "aggregates", "redirects",
			"requestResponse", "oneWay" );

	private static final List< String > OUTPUTPORTKEYWORDS =
		List.of( "location", "protocol", "interfaces", "aggregates", "redirects",
			"requestResponse", "oneWay" );

	private static final List< String > OUTERKEYWORDS =
		List.of( "service", "interface", "from", "include", "type", "import",
			"execution", "constants", "cset", "embedded", "currier", "public", "private", "extender" );

	private static final List< String > EXECUTIONKEYWORDS = List.of( "sequential", "concurrent", "single" );

	private static final List< String > SERVICEKEYWORDS =
		List.of( "execution", "init", "main", "inputPort", "outputPort", "embed", "as", "define" );

	private static final List< String > MAINKEYWORDS =
		List.of( "for", "while", "if", "else", "else if", "foreach", "with", "undef",
			"synchronized", "scope", "install", "spawn", "over", "in", "throw", "cH", "comp", "nullProcess" );

	private static final List< String > INTERFACEKEYWORDS = List.of( "oneWay", "requestResponse" );

	private static final List< String > EMBEDDEDKEYWORDS = List.of( "Jolie", "Java", "JavaScript" );

	private static final List< String > CURRIERKEYWORDS = List.of( "forward" );

	public static List< String > inputPortKeywords() {
		return INPUTPORTKEYWORDS;
	}

	public static List< String > outputPortKeywords() {
		return OUTPUTPORTKEYWORDS;
	}

	public static List< String > outerKeywords() {
		return OUTERKEYWORDS;
	}

	public static List< String > executionKeywords() {
		return EXECUTIONKEYWORDS;
	}

	public static List< String > serviceKeywords() {
		return SERVICEKEYWORDS;
	}

	public static List< String > mainKeywords() {
		return MAINKEYWORDS;
	}

	public static List< String > interfaceKeywords() {
		return INTERFACEKEYWORDS;
	}

	public static List< String > embeddedKeywords() {
		return EMBEDDEDKEYWORDS;
	}

	public static List< String > cuorierKeywords() {
		return CURRIERKEYWORDS;
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
			return List.of( "import" );
		default:
			return List.of();
		}
	}
}
