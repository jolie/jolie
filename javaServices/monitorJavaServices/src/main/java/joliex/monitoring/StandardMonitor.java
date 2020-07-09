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
 * @author Claudio Guidi 24/01/2012 supported events: start_session, stop_session, start_operation,
 *         stop_operation
 *
 */
public class StandardMonitor extends AbstractMonitorJavaService {
	private final Deque< MonitoringEvent > q = new LinkedList<>(); // event list
	private boolean triggerEnabled;
	private int queueMax;
	private int triggerThreshold;
	private boolean alert;
	private boolean directPushing;
	private String directPushingOperationName;

	public StandardMonitor() {
		triggerEnabled = false;
		triggerThreshold = 10;
		queueMax = 100;
		alert = false;
		directPushing = true;
		directPushingOperationName = "getMonitorEvent";
	}



	@Override
	public void pushEvent( MonitoringEvent e ) {
		// discard monitorAlert events
		if( directPushing ) {
			sendMessage( CommMessage.createRequest( directPushingOperationName, "/", MonitoringEvent.toValue( e ) ) );
		} else {

			synchronized( this ) {
				if( q.size() >= queueMax ) {
					q.removeFirst();
				}
				q.addLast( e );
				if( triggerEnabled && !alert ) {
					if( q.size() >= triggerThreshold ) {
						sendMessage( CommMessage.createRequest( "monitorAlert", "/", Value.create() ) );
						alert = true;
					}
				}
			}

		}
	}


	public Value flush() {
		synchronized( this ) {
			Iterator< MonitoringEvent > it = q.iterator();
			int index = 0;
			Value response = Value.create();
			while( it.hasNext() ) {
				MonitoringEvent e = it.next();
				response.getChildren( "events" ).get( index ).getFirstChild( MonitoringEvent.FieldNames.DATA.getName() )
					.deepCopy( e.data() );
				response.getChildren( "events" ).get( index )
					.getFirstChild( MonitoringEvent.FieldNames.MEMORY.getName() ).setValue( e.memory() );
				response.getChildren( "events" ).get( index )
					.getFirstChild( MonitoringEvent.FieldNames.TIMESTAMP.getName() ).setValue( e.timestamp() );
				response.getChildren( "events" ).get( index ).getFirstChild( MonitoringEvent.FieldNames.TYPE.getName() )
					.setValue( e.type() );
				response.getChildren( "events" ).get( index )
					.getFirstChild( MonitoringEvent.FieldNames.SERVICE.getName() ).setValue( e.service() );
				response.getChildren( "events" ).get( index )
					.getFirstChild( MonitoringEvent.FieldNames.CELLID.getName() ).setValue( e.cellId() );
				response.getChildren( "events" ).get( index )
					.getFirstChild( MonitoringEvent.FieldNames.SCOPE.getName() ).setValue( e.scope() );
				response.getChildren( "events" ).get( index )
					.getFirstChild( MonitoringEvent.FieldNames.PROCESSID.getName() ).setValue( e.processId() );
				if( e.parsingContext() != null ) {
					response.getChildren( "events" ).get( index ).getFirstChild( "context" )
						.getFirstChild( MonitoringEvent.FieldNames.CONTEXT_FILENAME.getName() )
						.setValue( e.parsingContext().sourceName() );
					response.getChildren( "events" ).get( index ).getFirstChild( "context" )
						.getFirstChild( MonitoringEvent.FieldNames.CONTEXT_LINE.getName() )
						.setValue( e.parsingContext().line() );
				}
				index++;
			}
			q.clear();
			alert = false;
			return response;
		}
	}



	/*
	 * request: .triggered_enabled?: bool .triggerThreshold?: int .queueMax?: int
	 *
	 */
	public void setMonitor( Value request ) {
		if( request.hasChildren( "triggeredEnabled" ) ) {
			triggerEnabled = request.getFirstChild( "triggeredEnabled" ).boolValue();
		}
		if( request.hasChildren( "triggerThreshold" ) ) {
			triggerThreshold = request.getFirstChild( "triggerThreshold" ).intValue();
		}
		if( request.hasChildren( "queueMax" ) ) {
			queueMax = request.getFirstChild( "queueMax" ).intValue();
		}

		if( request.hasChildren( "directPushing" ) ) {
			directPushing = request.getFirstChild( "directPushing" ).boolValue();
			if( request.getFirstChild( "directPushing" ).hasChildren( "operationName" ) ) {
				directPushingOperationName =
					request.getFirstChild( "directPushing" ).getFirstChild( "operationName" ).strValue();
			}
		}
	}

}
