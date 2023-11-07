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

package jolie.runtime.correlation;

import jolie.Interpreter;
import jolie.SessionListener;
import jolie.SessionThread;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.Value;
import jolie.runtime.correlation.CorrelationSet.CorrelationPair;
import jolie.runtime.correlation.impl.SimpleCorrelationEngine;

/**
 * Generic abstract class for correlation algorithm implementations.
 *
 * @author Fabrizio Montesi
 */
public abstract class CorrelationEngine implements SessionListener {
	public enum Type {
		SIMPLE {
			@Override
			public CorrelationEngine createInstance( Interpreter interpreter ) {
				return new SimpleCorrelationEngine( interpreter );
			}
		},
		HASH {
			@Override
			public CorrelationEngine createInstance( Interpreter interpreter ) {
				// return new HashCorrelationEngine( interpreter );
				return null;
			}
		};

		public abstract CorrelationEngine createInstance( Interpreter interpreter );

		public static Type fromString( String name ) {
			switch( name ) {
			case "simple":
				return SIMPLE;
			case "hash":
				return HASH;
			default:
				return null;
			}
		}
	}

	public abstract void onSessionStart( SessionThread session, Interpreter.SessionStarter starter,
		CommMessage message );

	public abstract void onSingleExecutionSessionStart( SessionThread session );

	protected abstract boolean routeMessage( CommMessage message, CommChannel channel );

	private final Interpreter interpreter;

	public CorrelationEngine( Interpreter interpreter ) {
		this.interpreter = interpreter;
	}

	protected Interpreter interpreter() {
		return interpreter;
	}

	protected void initCorrelationValues( SessionThread session, Interpreter.SessionStarter starter,
		CommMessage message ) {
		Value messageValue;
		CorrelationSet correlationSet = starter.correlationInitializer();
		if( correlationSet == null ) { // TODO check this w.r.t. the type system
			return;
		}
		for( CorrelationPair pair : starter.correlationInitializer()
			.getOperationCorrelationPairs( starter.guard().inputOperation().id() ) ) {
			messageValue = pair.messagePath().getValueOrNull( message.value() );
			if( messageValue == null ) {
				messageValue = Value.create();
			}
			pair.sessionPath().getValue( session.state().root() ).assignValue( messageValue );
		}
	}

	public synchronized void onMessageReceive( final CommMessage message, final CommChannel channel )
		throws CorrelationError {
		if( !(
		// We try to find a correlating process.
		routeMessage( message, channel )
			||
			// If there is none, we must be able to start a new process with this message.
			interpreter.startServiceSession( message, channel )) ) {
			// Otherwise, exception.
			throw new CorrelationError();
		}
	}
}
