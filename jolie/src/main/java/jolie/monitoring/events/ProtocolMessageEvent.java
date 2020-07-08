package jolie.monitoring.events;

import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

public class ProtocolMessageEvent extends MonitoringEvent {

	public enum Protocol {
		SOAP( "soap" ), HTTP( "http" );

		private String protocol;

		Protocol( String protocol ) {
			this.protocol = protocol;
		}

		public String getProtocol() {
			return protocol;
		}
	}

	public static enum FieldNames {
		PROTOCOL( "protocol" ), MESSAGE( "message" ), HEADER( "header" );

		private String fieldName;

		FieldNames( String name ) {
			this.fieldName = name;
		}

		public String getName() {
			return this.fieldName;
		}
	}

	public static String protocol( Value value ) {
		return value.getFirstChild( FieldNames.PROTOCOL.getName() ).strValue();
	}

	public static String message( Value value ) {
		return value.getFirstChild( FieldNames.MESSAGE.getName() ).strValue();
	}

	public static String header( Value value ) {
		return value.getFirstChild( FieldNames.HEADER.getName() ).strValue();
	}


	public ProtocolMessageEvent( String message, String header, String service,
		ProtocolMessageEvent.Protocol protocol, String processId, String scope, ParsingContext context ) {

		super( EventTypes.PROTOCOL_MESSAGE, service, scope, processId, context, Value.create() );

		data().getFirstChild( FieldNames.PROTOCOL.getName() ).setValue( protocol.getProtocol() );
		data().getFirstChild( FieldNames.HEADER.getName() ).setValue( header );
		data().getFirstChild( FieldNames.MESSAGE.getName() ).setValue( message );
	}
}
