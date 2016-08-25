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

package jolie.process;

import jolie.SessionContext;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class ForEachProcess implements Process
{
	private class UpdateKeyPathProcess implements Process {
		
		private final String id;
		private final VariablePath keyPath;

		public UpdateKeyPathProcess( String id, VariablePath keyPath )
		{
			this.id = id;
			this.keyPath = keyPath;
		}
		
		@Override
		public void run( SessionContext ctx ) throws FaultException, ExitingException
		{
			keyPath.getValue( ctx ).setValue( id );
		}

		@Override
		public Process clone( TransformationReason reason )
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}

		@Override
		public boolean isKillable()
		{
			return true;
		}
		
	}
	
	final private VariablePath keyPath, targetPath;
	final private Process process;

	public ForEachProcess(
			VariablePath keyPath,
			VariablePath targetPath,
			Process process )
	{
		this.keyPath = keyPath;
		this.targetPath = targetPath;
		this.process = process;
	}
	
	@Override
	public Process clone( TransformationReason reason )
	{
		return new ForEachProcess(
					(VariablePath) keyPath.cloneExpression( reason ),
					(VariablePath) targetPath.cloneExpression( reason ),
					process.clone( reason )
				);
	}
	
	@Override
	public void run(SessionContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		Value v = targetPath.getValueOrNull();
		if ( v != null && v.hasChildren() ) {
			String keys[];
			synchronized( v ) {
				keys = new String[ v.children().keySet().size() ];
				keys = v.children().keySet().toArray( keys );
			}
			
			for( int i = keys.length - 1; i >= 0; i-- ) {
				ctx.executeNext( new UpdateKeyPathProcess( keys[i], keyPath ), 
					process);
			}
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
