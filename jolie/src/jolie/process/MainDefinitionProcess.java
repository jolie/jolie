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

import jolie.Interpreter;
import jolie.deploy.OutputPort;
import jolie.runtime.EmbeddedServiceLoader;
import jolie.runtime.EmbeddedServiceLoadingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;



public class MainDefinitionProcess extends SubRoutineProcess
{
	public MainDefinitionProcess()
	{
		super( "main" );
	}
	
	public void run()
		throws FaultException
	{
		try {
			Interpreter interpreter = Interpreter.getInstance();
			
			for( OutputPort outputPort : Interpreter.getInstance().outputPorts() ) {
				try {
					outputPort.configurationProcess().run();
				} catch( FaultException fe ) {
					// If this happens, it's a bug in the SemanticVerifier
					assert( false );
				}
			}
			
			for( EmbeddedServiceLoader loader : interpreter.embeddedServiceLoaders() )
				loader.load();
			
			for( Process p : interpreter.commCore().protocolConfigurations() )
				p.run();
		
			try {
				SubRoutineProcess p = interpreter.getDefinition( "init" );
				p.run();
			} catch( InvalidIdException e ) {}
			
			super.run();
		} catch( EmbeddedServiceLoadingException e ) {
			Interpreter.getInstance().logger().severe( e.getMessage() );
		}
	}
}