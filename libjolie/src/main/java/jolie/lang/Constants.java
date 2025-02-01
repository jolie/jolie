/*
 * Copyright (C) 2008-2020 Fabrizio Montesi <famontesi@gmail.com>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

import jolie.lang.parse.Scanner;
import jolie.util.Range;

/**
 * Global constants.
 *
 * @author Fabrizio Montesi
 */
public final class Constants {
	// Release information
	public static final String VERSION = readVersionFromProperties();
	public static final String COPYRIGHT =
		String.format( "(C) 2006-%4d the Jolie developers", Calendar.getInstance().get( Calendar.YEAR ) );
	public static final String ASCII_LOGO = readAsciiLogo();

	private static String readVersionFromProperties() {
		try( InputStream is = Constants.class.getClassLoader().getResourceAsStream( "libjolie.properties" ) ) {
			if( is == null ) {
				return "ERROR: could not find libjolie.properties. Your distribution of Jolie might be corrupted.";
			}
			Properties prop = new Properties();
			try {
				prop.load( is );
			} catch( IOException e ) {
				return "ERROR: could not read libjolie.properties. Your distribution of Jolie might be corrupted.";
			}
			return prop.getProperty( "jolie.version" );
		} catch( IOException e ) {
			return "ERROR: could not close libjolie.properties correctly.";
		}
	}

	private static String readAsciiLogo() {
		try( BufferedReader reader = new BufferedReader(
			new InputStreamReader( Constants.class.getClassLoader().getResourceAsStream( "ascii-logo.txt" ) ) ) ) {
			return reader.lines().collect( Collectors.joining( "\n" ) );
		} catch( IOException e ) {
			return "ERROR: could not close ascii-logo.txt correctly.";
		}
	}

	public interface Manifest {
		// Jolie Extensions
		Attributes.Name CHANNEL_EXTENSION = new Attributes.Name( "X-JOLIE-ChannelExtension" );
		Attributes.Name LISTENER_EXTENSION = new Attributes.Name( "X-JOLIE-ListenerExtension" );
		Attributes.Name PROTOCOL_EXTENSION = new Attributes.Name( "X-JOLIE-ProtocolExtension" );
		Attributes.Name EMBEDDING_EXTENSION = new Attributes.Name( "X-JOLIE-EmbeddingExtension" );

		// JAP Manifest
		Attributes.Name MAIN_PROGRAM = new Attributes.Name( "X-JOLIE-Main-Program" );
		Attributes.Name OPTIONS = new Attributes.Name( "X-JOLIE-Options" );
		// public final static String Libraries = "X-JOLIE-Libraries";
	}

	public enum Predefined {
		ATTRIBUTES( "@Attributes", "@Attributes" ), HTTP_BASIC_AUTHENTICATION( "@HttpBasicAuthentication",
			"@HttpBasicAuthentication" ), PI( "PI", java.lang.Math.PI );

		private final String id;
		private final Scanner.Token token;

		public static Predefined get( String id ) {
			for( Predefined p : Predefined.values() ) {
				if( p.id.equals( id ) ) {
					return p;
				}
			}
			return null;
		}

		Predefined( String id, String content ) {
			this.id = id;
			this.token = new Scanner.Token( Scanner.TokenType.STRING, content );
		}

		Predefined( String id, Integer content ) {
			this.id = id;
			this.token = new Scanner.Token( Scanner.TokenType.INT, content.toString() );
		}

		Predefined( String id, Double content ) {
			this.id = id;
			this.token = new Scanner.Token( Scanner.TokenType.DOUBLE, content.toString() );
		}

		public final String id() {
			return id;
		}

		public final Scanner.Token token() {
			return token;
		}
	}

	public final static Range RANGE_ONE_TO_ONE = new Range( 1, 1 );

	public static final String DEFAULT_HANDLER_NAME = "default";
	public static final String TYPE_MISMATCH_FAULT_NAME = "TypeMismatch";
	public static final String IO_EXCEPTION_FAULT_NAME = "IOException";
	public static final String TIMEOUT_EXCEPTION_FAULT_NAME = "Timeout";

	public static final String MONITOR_OUTPUTPORT_NAME = "#Monitor";
	public static final String INPUT_PORTS_NODE_NAME = "inputPorts";
	public static final String PROTOCOL_NODE_NAME = "protocol";
	public static final String LOCATION_NODE_NAME = "location";
	public static final String LOCAL_LOCATION_KEYWORD = "local";
	public static final String LOCAL_INPUT_PORT_NAME = "LocalInputPort";
	// public static String newLineString = System.getProperty( "line.separator" );
	public static final String FILE_SEPARATOR = System.getProperty( "file.separator" );
	public static final String PATH_SEPARATOR = System.getProperty( "path.separator" );
	public static final String ROOT_RESOURCE_PATH = "/";
	public static final String JOLIE_LOGGER_NAME = "Jolie";

	public enum EmbeddedServiceType {
		JOLIE( "Jolie" ), JAVA( "Java" ), JAVASCRIPT( "JavaScript" ), SERVICENODE( "ServiceNode" ), SERVICENODE_JAVA(
			"ServiceNodeJava" ), INTERNAL(
				"JolieInternal" ), UNSUPPORTED(
					"Unsupported" );

		private final String str;

		EmbeddedServiceType( String str ) {
			this.str = str;
		}

		@Override
		public String toString() {
			return str;
		}
	}

	public static EmbeddedServiceType stringToEmbeddedServiceType( String str ) {
		switch( str.toLowerCase() ) {
		case "jolie":
			return EmbeddedServiceType.JOLIE;
		case "java":
			return EmbeddedServiceType.JAVA;
		case "javascript":
			return EmbeddedServiceType.JAVASCRIPT;
		case "servicenode":
			return EmbeddedServiceType.SERVICENODE;
		case "servicenode-java":
			return EmbeddedServiceType.SERVICENODE_JAVA;
		default:
			return EmbeddedServiceType.UNSUPPORTED;
		}
	}

	public enum ExecutionMode {
		SINGLE, SEQUENTIAL, CONCURRENT
	}

	public enum OperationType {
		ONE_WAY, REQUEST_RESPONSE
	}

	public enum OperandType {
		ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULUS
	}

	public static long serialVersionUID() {
		return 1L;
	}

	public static final int TAB_SIZE = 4;

	public static final String PACKAGE_DIR = "packages";
}
