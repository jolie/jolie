/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jolie.monitoring.events;

import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

/**
 *
 * @author claudio guidi 17/02/2012
 */
public class OperationEndedEvent extends MonitoringEvent {
	public static final int SUCCESS = 0;
	public static final int FAULT = 1;
	public static final int ERROR = 2;

	public OperationEndedEvent( String operationName, String processId, String messageId, int status, String details,
		Value value, String service, ParsingContext context ) {

		super( "OperationEnded", service, context, Value.create() );

		data().getFirstChild( "operationName" ).setValue( operationName );
		data().getFirstChild( "processId" ).setValue( processId );
		data().getFirstChild( "messageId" ).setValue( messageId );
		data().getFirstChild( "status" ).setValue( status );
		data().getFirstChild( "details" ).setValue( details );
		data().getFirstChild( "value" ).deepCopy( value );

	}

}
