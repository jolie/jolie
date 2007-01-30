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

import java.util.Vector;

public class OutputOperation extends Operation
{
	private String boundOperationId;
	private Vector< Variable.Type > outVarTypes;

	public OutputOperation( String id,
			String boundOperationId,
			Vector< Variable.Type > outVarTypes
			)
	{
		super( id );
		this.boundOperationId = boundOperationId;
		this.outVarTypes = outVarTypes;
	}
	
	public Vector< Variable.Type > outVarTypes()
	{
		return outVarTypes;
	}
	
	public String boundOperationId()
	{
		return boundOperationId;
	}
	
	public static OutputOperation getById( String id )
		throws InvalidIdException
	{
		Operation obj = Operation.getById( id );
		if ( !( obj instanceof OutputOperation ) )
			throw new InvalidIdException( id );
		return (OutputOperation)obj;
	}
}
