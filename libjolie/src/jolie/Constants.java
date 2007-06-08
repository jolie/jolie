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

public final class Constants
{
	public static final String VERSION = "JOLIE 0.9.1";
	public static final String COPYRIGHT = "(C) 2006-2007 the JOLIE team";

	public enum StateMode {
		PERSISTENT, NOT_PERSISTENT
	}
	
	public enum ExecutionMode {
		SEQUENTIAL, CONCURRENT
	}
	
	public enum OperandType {
		ADD, SUBTRACT,
		MULTIPLY, DIVIDE
	}
	
	public enum ProtocolId {
		UNSUPPORTED,
		SODEP,
		SOAP
	}
	
	public enum MediumId {
		UNSUPPORTED,
		SOCKET
	}
	
	public enum VariableType {
		UNDEFINED,	///< Undefined variable.
		INT,		///< Integer variable. Used also by operations for type management.
		STRING,		///< String variable. Used also by operations for type management.
		VARIANT		///< Variant variable. Used only by operations for type management.
	}
	
	public static long serialVersionUID() { return 1L; }
	
	public static ProtocolId stringToProtocolId( String str )
	{
		if ( str.equals( "soap" ) )
			return ProtocolId.SOAP;
		else if ( str.equals( "sodep" ) )
			return ProtocolId.SODEP;
		
		return ProtocolId.UNSUPPORTED;
	}
	
	public static MediumId stringToMediumId( String str )
	{
		if ( str.equals( "socket" ) )
			return MediumId.SOCKET;
		
		return MediumId.UNSUPPORTED;
	}
}
