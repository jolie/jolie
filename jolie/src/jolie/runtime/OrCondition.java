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


package jolie.runtime;


import jolie.process.TransformationReason;

public class OrCondition implements Condition
{
	final private Condition[] children;
	
	public OrCondition( Condition[] children )
	{
		this.children = children;
	}
	
	public Condition cloneCondition( TransformationReason reason )
	{
		return new OrCondition( children );
	}
	
	public boolean evaluate()
	{
		for( Condition cond : children ) {
			if ( cond.evaluate() ) {
				return true;
			}
		}

		return false;
	}
}
