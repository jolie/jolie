package jolie.monitoring.events;

import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

public class FaultHandlerEndEvent extends MonitoringEvent {

	public static enum FieldNames {
		FAULTNAME( "faultname" );

		private String fieldName;

		FieldNames( String name ) {
			this.fieldName = name;
		}

		public String getName() {
			return this.fieldName;
		}
	}

	public static String faultname( Value value ) {
		return value.getFirstChild( FieldNames.FAULTNAME.getName() ).strValue();
	}

	public FaultHandlerEndEvent( String service, String processId, String scope, String faultname,
		ParsingContext context ) {

		super( EventTypes.FAULT_HANDLER_END, service, scope, processId, context, Value.create() );

		data().getFirstChild( FieldNames.FAULTNAME.getName() ).setValue( faultname );
	}
}
