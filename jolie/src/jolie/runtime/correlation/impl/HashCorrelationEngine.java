/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.runtime.correlation.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jolie.Interpreter;
import jolie.SessionThread;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.SessionMessage;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.correlation.CorrelationEngine;
import jolie.runtime.correlation.CorrelationSet;
import jolie.runtime.correlation.CorrelationSet.CorrelationPair;

/**
 * A hash table based correlation algorithm.
 * @author Fabrizio Montesi
 */
public class HashCorrelationEngine extends CorrelationEngine
{
	private final Set< SessionThread > sessions = new HashSet< SessionThread >();

	public HashCorrelationEngine( Interpreter interpreter )
	{
		super( interpreter );
	}

	public synchronized boolean routeMessage( CommMessage message, CommChannel channel )
	{
		for( SessionThread session : sessions ) {
			if ( correlate( session, message ) ) {
				session.pushMessage( new SessionMessage( message, channel ) );
				return true;
			}
		}
		return false;
	}

	public synchronized void onSingleExecutionSessionStart( SessionThread session )
	{
		sessions.add( session );
	}

	public synchronized void onSessionStart( SessionThread session, Interpreter.SessionStarter starter, CommMessage message )
	{
		sessions.add( session );
		initCorrelationValues( session, starter, message );
	}

	public synchronized void onSessionExecuted( SessionThread session )
	{
		sessions.remove( session );
	}

	public synchronized void onSessionError( SessionThread session, FaultException fault )
	{
		onSessionExecuted( session );
	}

	private boolean correlate( SessionThread session, CommMessage message )
	{
		Value sessionValue;
		Value messageValue;
		CorrelationPair cpair;
		int i;
		List< CorrelationPair > pairs;
		boolean matches = true;
		for( CorrelationSet cset : interpreter().correlationSets() ) {
			pairs = cset.getOperationCorrelationPairs( message.operationName() );
			if ( pairs == null ) {
				matches = false;
			} else {
				for( i = 0; i < pairs.size() && matches; i++ ) {
					cpair = pairs.get( i );
					sessionValue = cpair.sessionPath().getValueOrNull( session.state().root() );
					if ( sessionValue == null ) {
						matches = false;
					} else {
						messageValue = cpair.messagePath().getValueOrNull( message.value() );
						if ( messageValue == null ) {
							matches = false;
						} else {
							// TODO: Value.equals is type insensitive, fix this with an additional check.
							matches = sessionValue.equals( messageValue );
						}
					}
				}
			}

			if ( matches ) {
				// The correlation set matched.
				return true;
			} else {
				// The correlation set did not match, let us try another one.
				matches = true;
			}
		}

		// We could not find any correlation set matching.
		return false;
	}
}
