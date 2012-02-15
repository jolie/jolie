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

	public OperationEndedEvent( String operation_name, String session_id ) {

		super( "OperationEnded", Value.create() );

		data().getFirstChild( "operation_name" ).setValue( operation_name );
		data().getFirstChild( "session_id" ).setValue( "session_id" );

	}

}
