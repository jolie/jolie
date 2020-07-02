package jolie.monitoring.events;

import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

public class LogEvent extends MonitoringEvent {

	public enum LogLevel {
		INFO( "INFO" ), WARNING( "WARNING" ), ERROR( "ERROR" );

		private String level;

		LogLevel( String level ) {
			this.level = level;
		}

		public String getLevel() {
			return level;
		}
	}

	public LogEvent( String message, String service, LogLevel logLevel, String processId, String type, String scope,
		ParsingContext context ) {

		super( "Log", service, scope, context, Value.create() );

		data().getFirstChild( "level" ).setValue( logLevel.getLevel() );
		data().getFirstChild( "message" ).setValue( message );
		data().getFirstChild( "processId" ).setValue( processId );
		data().getFirstChild( "extendedType" ).setValue( type );
	}

	public LogEvent( String message, String service, LogLevel logLevel, String processId, String scope,
		ParsingContext context ) {

		super( "Log", service, scope, context, Value.create() );

		data().getFirstChild( "level" ).setValue( logLevel.getLevel() );
		data().getFirstChild( "message" ).setValue( message );
		data().getFirstChild( "processId" ).setValue( processId );
	}
}
