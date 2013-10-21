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
 * 17/02/2012
 */
public class OperationEndedEvent extends MonitoringEvent {
	public static final int SUCCESS = 0;
	public static final int FAULT = 1;
	public static final int ERROR = 2;

	public OperationEndedEvent( String operation_name, String session_id, int status, String details, Value message ) {

		super( "OperationEnded", Value.create() );

		data().getFirstChild( "operation_name" ).setValue( operation_name );
		data().getFirstChild( "session_id" ).setValue( session_id );
		data().getFirstChild( "status" ).setValue( status );
		data().getFirstChild( "details" ).setValue( details );
                data().getFirstChild( "message" ).deepCopy( message );

	}

}
