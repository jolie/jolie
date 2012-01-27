/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jolie.monitoring.events;

import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

/**
 *
 * @author claudio guidi
 * 27/01/2012
 */
public class SessionEndedEvent extends MonitoringEvent {
	
	public SessionEndedEvent( String operation_name, String session_id )
	{
		super( "SessionEnded", Value.UNDEFINED_VALUE );

		data().getFirstChild( "session_id" ).setValue( session_id );
		data().getFirstChild( "operation_name" ).setValue( operation_name );

	}
}
