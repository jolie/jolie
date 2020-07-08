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

	public static enum FieldNames {
		OPERATION_NAME( "operationName" ), MESSAGE_ID( "messageId" ), STATUS( "status" ), DETAILS( "details" ), VALUE(
			"value" );

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

	public static String messageId( Value value ) {
		return value.getFirstChild( FieldNames.MESSAGE_ID.getName() ).strValue();
	}

	public static Integer status( Value value ) {
		return value.getFirstChild( FieldNames.STATUS.getName() ).intValue();
	}

	public static String setails( Value value ) {
		return value.getFirstChild( FieldNames.DETAILS.getName() ).strValue();
	}

	public static Value value( Value value ) {
		return value.getFirstChild( FieldNames.VALUE.getName() );
	}


	public OperationEndedEvent( String operationName, String processId, String messageId, int status, String details,
		Value value, String service, String scope, ParsingContext context ) {

		super( EventTypes.OPERATION_ENDED, service, scope, processId, context, Value.create() );

		data().getFirstChild( FieldNames.OPERATION_NAME.getName() ).setValue( operationName );
		data().getFirstChild( FieldNames.MESSAGE_ID.getName() ).setValue( messageId );
		data().getFirstChild( FieldNames.STATUS.getName() ).setValue( status );
		data().getFirstChild( FieldNames.DETAILS.getName() ).setValue( details );
		data().getFirstChild( FieldNames.VALUE.getName() ).deepCopy( value );

	}

}
