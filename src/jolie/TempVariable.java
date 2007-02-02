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



public class TempVariable extends Variable
{
	public TempVariable()
	{
		super();
	}
	
	public TempVariable( String value )
	{
		super();
		setStrValue( value );
	}
	
	public TempVariable( int value )
	{
		super();
		setIntValue( value );
	}
	
	public TempVariable( Variable variable )
	{
		super( variable );
	}
	
	/*public static Vector< TempVariable > createTypedVars(
			Vector< Variable.Type > typesVec,
			Vector< Variable > varsVec )
	{
		assert typesVec.size() == varsVec.size();
		
		Vector< TempVariable > retVec = new Vector< TempVariable >();
		int i = 0;
		TempVariable tempVar;
		for( Variable.Type type : typesVec ) {
			if ( type == Variable.Type.INT )
				tempVar = new TempVariable( varsVec.elementAt( i ).intValue() );
			else if ( type == Variable.Type.STRING )
				tempVar = new TempVariable( varsVec.elementAt( i ).strValue() );
			else
				tempVar = new TempVariable( varsVec.elementAt( i ) );
			
			retVec.add( tempVar );
		}
		return retVec;
	}*/
}
