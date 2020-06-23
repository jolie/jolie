package jolie.monitoring.events;

import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

public class LogEvent extends MonitoringEvent {

	public enum LogLevel {
		INFO( "info" ), WARNING( "warning" ), ERROR( "error" );

		private String level;

		LogLevel( String level ) {
			this.level = level;
		}

		public String getLevel() {
			return level;
		}
	}

	public LogEvent( String message, String service, LogLevel logLevel, ParsingContext context ) {

		super( "Log", service, context, Value.create() );

		data().getFirstChild( "level" ).setValue( logLevel.getLevel() );
		data().getFirstChild( "message" ).setValue( message );
	}
}
