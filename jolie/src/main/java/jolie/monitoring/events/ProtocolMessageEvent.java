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

	public ProtocolMessageEvent( String message, String header, String service,
		ProtocolMessageEvent.Protocol protocol, String processId, ParsingContext context ) {

		super( "ProtocolMessageEvent", service, context, Value.create() );

		data().getFirstChild( "protocol" ).setValue( protocol.getProtocol() );
		data().getFirstChild( "header" ).setValue( header );
		data().getFirstChild( "message" ).setValue( message );
		data().getFirstChild( "processId" ).setValue( processId );
	}
}
