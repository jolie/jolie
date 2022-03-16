package jolie.monitoring.events;

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
		PROTOCOL( "protocol" ), MESSAGE( "message" ), HEADER( "header" ), BODY( "body" );

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

	public static String body( Value value ) {
		return value.getFirstChild( FieldNames.BODY.getName() ).strValue();
	}

	public ProtocolMessageEvent( String message, String body, String header, ProtocolMessageEvent.Protocol protocol ) {
		super( "ProtocolMessage - " + protocol.getProtocol(), Value.create() );

		data().getFirstChild( FieldNames.PROTOCOL.getName() ).setValue( protocol.getProtocol() );
		data().getFirstChild( FieldNames.HEADER.getName() ).setValue( header );
		data().getFirstChild( FieldNames.MESSAGE.getName() ).setValue( message );
		data().getFirstChild( FieldNames.BODY.getName() ).setValue( body );
	}
}
