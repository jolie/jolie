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
 * @author claudio guidi 27/01/2012
 *
 *         ending states:
 *
 */
public class SessionEndedEvent extends MonitoringEvent {

	public SessionEndedEvent( String operationName, String processId, String service, String scope,
		ParsingContext context ) {
		super( "SessionEnded", service, scope, context, Value.create() );

		data().getFirstChild( "processId" ).setValue( processId );
		data().getFirstChild( "operationName" ).setValue( operationName );

	}
}
