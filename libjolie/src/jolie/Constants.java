/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import jolie.lang.parse.Scanner;


public final class Constants
{
	static public enum Predefined {
		ATTRIBUTES( "@Attributes", "@Attributes" ),
		PI( "PI", java.lang.Math.PI );
		
		private final String id;
		private final Scanner.Token token;
		
		public static Predefined get( String id )
		{
			for( Predefined p : Predefined.values() ) {
				if ( p.id.equals( id ) )
					return p;
			}
			return null;
		}
		
		Predefined( String id, String content )
		{
			this.id = id;
			this.token = new Scanner.Token( Scanner.TokenType.STRING, content );
		}
		
		Predefined( String id, Integer content )
		{
			this.id = id;
			this.token = new Scanner.Token( Scanner.TokenType.INT, content.toString() );
		}
		
		Predefined( String id, Double content )
		{
			this.id = id;
			this.token = new Scanner.Token( Scanner.TokenType.REAL, content.toString() );
		}
		
		public String id()
		{
			return id;
		}
		
		public Scanner.Token token()
		{
			return token;
		}
	}
	
	public final static Charset stringCharset;
	static {
		stringCharset = Charset.forName( "UTF8" );
	}

	public static final String VERSION = "JOLIE 1.0_tp2";
	public static final String COPYRIGHT = "(C) 2006-2007-2008 the JOLIE team";
	//public static String newLineString = System.getProperty( "line.separator" );
	public static String fileSeparator = System.getProperty( "file.separator" );
	public static String pathSeparator = System.getProperty( "path.separator" );
	public static final String GLOBAL = "global";
	
	public enum EmbeddedServiceType {
		JAVA, JOLIE, UNSUPPORTED
	}
	
	public static EmbeddedServiceType stringToEmbeddedServiceType( String str )
	{
		if ( "jolie".equalsIgnoreCase( str ) )
			return EmbeddedServiceType.JOLIE;
		else if ( "java".equalsIgnoreCase( str ) )
			return EmbeddedServiceType.JAVA;
		
		return EmbeddedServiceType.UNSUPPORTED;
	}
	
	public enum ExecutionMode {
		SINGLE, SEQUENTIAL, CONCURRENT
	}
	
	public enum OperandType {
		ADD, SUBTRACT,
		MULTIPLY, DIVIDE
	}
	
	public enum ProtocolId {
		UNSUPPORTED,
		SODEP,
		SOAP,
		HTTP
	}

	public enum MediumId {
		UNSUPPORTED,
		SOCKET,
		PIPE,
		FILE,
		DBUS
	}
	
	/**
	 * Pay attention that every type has a different byte identifier!
	 */
	static public enum ValueType {
		UNDEFINED((byte)0) {
			public Object readObject( DataInput in ) throws IOException {
				return null;
			}
			public void writeObject( DataOutput out, Object obj ) throws IOException {}
		},
		STRING((byte)1) {
			public Object readObject( DataInput in ) throws IOException {
				int len = in.readInt();
				if ( len > 0 ) {
					byte[] bb = new byte[ len ];
					in.readFully( bb );
					return new String( bb, jolie.Constants.stringCharset );
				}
				return "";
			}
			public void writeObject( DataOutput out, Object obj ) throws IOException {
				String str = (String)obj;
				if ( str.isEmpty() ) {
					out.writeInt( 0 );
				} else {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					Writer writer = new OutputStreamWriter( bos, jolie.Constants.stringCharset );
					writer.write( str );
					writer.close();
					byte[] bb = bos.toByteArray();
					out.writeInt( bb.length );
					out.write( bb );
				}
			}
		},
		INT((byte)2) {
			public Object readObject( DataInput in ) throws IOException {
				return in.readInt();
			}
			public void writeObject( DataOutput out, Object obj ) throws IOException {
				out.writeInt( (Integer)obj );
			}
		},
		DOUBLE((byte)3) {
			public Object readObject( DataInput in ) throws IOException {
				return in.readDouble();
			}
			public void writeObject( DataOutput out, Object obj ) throws IOException {
				out.writeDouble( (Double)obj );
			}
		};

		abstract public Object readObject( DataInput in )
			throws IOException;
		abstract public void writeObject( DataOutput out, Object obj )
			throws IOException;

		public static ValueType readType( DataInput in )
			throws IOException
		{
			Byte b = in.readByte();
			ValueType ret;
			if( (ret=typeMap.get( b )) == null )
				return UNDEFINED;
			return ret;
		}
		
		public void writeType( DataOutput out )
			throws IOException
		{
			out.write( id );
		}

		private static Map< Byte, ValueType > typeMap =
				new HashMap< Byte, ValueType >();
		private byte id;
		
		ValueType( byte id )
		{
			this.id = id;
		}
		
		static {
			for( ValueType t : ValueType.values() )
				typeMap.put( t.id, t );
		}
		
		public static ValueType fromObject( Object obj )
		{
			if ( obj == null )
				return UNDEFINED;
			else if ( obj instanceof Integer )
				return INT;
			else if ( obj instanceof String )
				return STRING;
			else if ( obj instanceof Double )
				return DOUBLE;
			
			return UNDEFINED;
		}
	}
	
	public static long serialVersionUID() { return 1L; }
	
	public static ProtocolId stringToProtocolId( String str )
	{
		if ( "soap".equals( str ) )
			return ProtocolId.SOAP;
		else if ( "sodep".equals( str ) )
			return ProtocolId.SODEP;
		else if ( "http".equals( str ) )
			return ProtocolId.HTTP;
		
		return ProtocolId.UNSUPPORTED;
	}
	
	public static MediumId stringToMediumId( String str )
	{
		if ( "socket".equals( str ) )
			return MediumId.SOCKET;
		else if ( "pipe".equals( str ) )
			return MediumId.PIPE;
		else if ( "file".equals( str ) )
			return MediumId.FILE;
		else if ( "dbus".equals( str ) )
			return MediumId.DBUS;
		
		return MediumId.UNSUPPORTED;
	}
}
