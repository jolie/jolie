/***************************************************************************
 *   Copyright (C) 2007-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.util.List;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.runtime.HandlerInstallationReason;
import jolie.tracer.FaultTraceAction;
import jolie.tracer.Tracer;
import jolie.util.Pair;


public class InstallProcess implements Process
{
	// The compensation is identified by an empty string
	private final List< Pair< String, Process > > pairs;

	public InstallProcess( List< Pair< String, Process > > pairs )
	{
		this.pairs = pairs;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new InstallProcess( pairs );
	}

	public void run()
	{
		final ExecutionThread ethread = ExecutionThread.currentThread();
		for( Pair< String, Process > pair : pairs ) {
			final Process handler = pair.value().clone( new HandlerInstallationReason( pair.key() ) );
			if ( pair.key() == null ) {
				ethread.installCompensation( handler );
                            log("INSTALLED", handler.toString());
			} else {
				ethread.installFaultHandler( pair.key(), handler );
                            log("INSTALLED", handler+" - " +pair.key());
			}
		}
	}
	
	public boolean isKillable()
	{
		return false;
	}
           private void log(String message, String value)
	{
		final Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace(() -> new FaultTraceAction(
                        ExecutionThread.currentThread().getSessionId(),
			FaultTraceAction.Type.FAULT_INSTALL,
			message,
                        value,
                        System.currentTimeMillis()
		) );
	} 
}
