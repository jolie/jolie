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

public interface Variable extends Expression
{
	public enum Type {
		UNDEFINED,	///< Undefined variable.
		INT,		///< Integer variable. Used also by operations for type management.
		STRING,		///< String variable. Used also by operations for type management.
		VARIANT		///< Variant variable. Used only by operations for type management.
	}
		
	public String id();
	public void setStrValue( String value );
	public boolean isDefined();
	public void setIntValue( int value );
	public String strValue();
	public int intValue();
	public void assignValue( Variable var );
	public Type type();
	public void add( Variable var );
	public void subtract( Variable var );
	public void multiply( Variable var );
	public void divide( Variable var );
	public boolean isString();
	public boolean isInt();
}