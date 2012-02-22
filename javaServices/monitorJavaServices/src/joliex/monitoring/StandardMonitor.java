/***************************************************************************
 *   Copyright (C) 2012 by Claudio Guidi <cguidi@italianasoftware.com>     *
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

package joliex.monitoring;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import jolie.monitoring.MonitoringEvent;
import jolie.net.CommMessage;
import jolie.runtime.Value;

/**
 *
 * @author Claudio Guidi 24/01/2012
 * supported events: start_session, stop_session, start_operation, stop_operation
 *
 */
public class StandardMonitor extends AbstractMonitorJavaService
{
	private Deque q = new LinkedList();		// event list
	private boolean triggerEnabled;
	private int queueMax;
	private int triggerThreshold;
	private boolean alert;

	public StandardMonitor()
	{
		triggerEnabled = false;
		triggerThreshold = 75;
		queueMax = 100;
		alert = false;
	}



	@Override
	public void pushEvent( MonitoringEvent e )
	{
		
		synchronized( this ) {
			if ( q.size() >= queueMax ) {
				q.removeFirst();
			}
			q.addLast( e );
			if ( triggerEnabled && !alert ) {
				if ( q.size() >= triggerThreshold ) {
					sendMessage( CommMessage.createRequest( "monitorAlert", "/", Value.create( ) ) );
					alert = true;
				}
			}
		}
	}


	public Value flush() {
		synchronized( this ) {
			Iterator<MonitoringEvent> it = q.iterator();
			int index = 0;
			Value response = Value.create();
			while ( it.hasNext() ) {
				MonitoringEvent e = it.next();
				response.getChildren( "events" ).get( index ).getFirstChild( "data" ).deepCopy( e.data() );
				response.getChildren( "events" ).get( index ).getFirstChild( "memory" ).setValue( e.memory() );
				response.getChildren( "events" ).get( index ).getFirstChild( "timestamp" ).setValue( e.timestamp() );
				response.getChildren( "events" ).get( index ).getFirstChild( "type" ).setValue( e.type() );
				index++;
			}
			q.clear();
			alert = false;
			return response;
		}
	}



	/*
	 * request:
	 *	   .triggered_enabled?: bool
	 *	   .triggerThreshold?: int
	 *     .queueMax?: int
	 *
	 */
	public void setMonitor( Value request ) {
		if ( request.getFirstChild( "triggeredEnabled" ).isDefined() ) {
			triggerEnabled = request.getFirstChild( "triggeredEnabled" ).boolValue();
		}
		if ( request.getFirstChild( "triggerThreshold" ).isDefined() ) {
			triggerThreshold = request.getFirstChild( "triggerThreshold" ).intValue();
		}
		if ( request.getFirstChild( "queueMax" ).isDefined() ) {
			queueMax = request.getFirstChild( "queueMax" ).intValue();
		}
	}

}
