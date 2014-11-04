/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.monitoring.events;

import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

/**
 *
 * @author claudio
 */
public class OperationCallEvent extends MonitoringEvent
{
	public static final int SUCCESS = 0;
	public static final int FAULT = 1;
	
	
	public OperationCallEvent( String operationName, String processId, String messageId, int status, String details, String outputPort, Value message ) {
		super( "OperationCall", Value.create() );
		
		data().getFirstChild( "operationName" ).setValue( operationName );
		data().getFirstChild( "processId" ).setValue( processId );
		data().getFirstChild( "messageId" ).setValue( messageId );
		data().getFirstChild( "status" ).setValue( status );
		data().getFirstChild( "details" ).setValue( details );
		data().getFirstChild( "outputPort" ).setValue( outputPort );
        data().getFirstChild( "message" ).deepCopy( message );
	}
}
