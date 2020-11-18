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

	public static enum FieldNames {
		OPERATION_NAME( "operationName" );

		private String fieldName;

		FieldNames( String name ) {
			this.fieldName = name;
		}

		public String getName() {
			return this.fieldName;
		}
	}

	public static String operationName( Value value ) {
		return value.getFirstChild( FieldNames.OPERATION_NAME.getName() ).strValue();
	}

	public SessionEndedEvent( String operationName, String processId, String service, String scope,
		ParsingContext context ) {
		super( EventTypes.SESSION_ENDED, service, scope, processId, context, Value.create() );
		data().getFirstChild( FieldNames.OPERATION_NAME.getName() ).setValue( operationName );

	}
}
