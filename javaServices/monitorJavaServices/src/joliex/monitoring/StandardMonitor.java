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
	private boolean trigger_enabled;
	private int queue_max;
	private int trigger_threshold;
	private boolean alert;

	public StandardMonitor()
	{
		trigger_enabled = false;
		trigger_threshold = 75;
		queue_max = 100;
		alert = false;
	}



	@Override
	public void pushEvent( MonitoringEvent e )
	{
		synchronized( this ) {
			if ( q.size() >= queue_max ) {
				q.removeFirst();
			}
			q.addLast( e );
			if ( trigger_enabled && !alert ) {
				if ( q.size() >= trigger_threshold ) {
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
				response.getChildren( "events" ).get( index ).deepCopy( it.next().data() );
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
	 *	   .trigger_threshold?: int
	 *     .queue_max?: int
	 *
	 */
	public void setMonitor( Value request ) {
		if ( request.getFirstChild( "triggered_enabled" ).isDefined() ) {
			trigger_enabled = request.getFirstChild( "triggered_enabled" ).boolValue();
		}
		if ( request.getFirstChild( "trigger_threshold" ).isDefined() ) {
			trigger_threshold = request.getFirstChild( "trigger_threshold" ).intValue();
		}
		if ( request.getFirstChild( "queue_max" ).isDefined() ) {
			queue_max = request.getFirstChild( "queue_max" ).intValue();
		}
	}

}
