/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jolie.monitoring.events;

import jolie.StatefulContext;
import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

/**
 *
 * @author claudio guidi
 * 27/01/2012
 *
 * ending states:
 *
 */
public class SessionEndedEvent extends MonitoringEvent {
	
	public SessionEndedEvent( StatefulContext sessionContext, String operationName )
	{
		super( "SessionEnded", Value.create(), sessionContext );

		data().getFirstChild( "processId" ).setValue( sessionContext.getSessionId() );
		data().getFirstChild( "operationName" ).setValue( operationName );

	}
}
