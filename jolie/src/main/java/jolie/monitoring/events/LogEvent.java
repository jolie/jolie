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

	public static enum FieldNames {
		LEVEL( "level" ), MESSAGE( "message" ), EXTENDED_TYPE( "extendedType" );

		private String fieldName;

		FieldNames( String name ) {
			this.fieldName = name;
		}

		public String getName() {
			return this.fieldName;
		}
	}

	public static String level( Value value ) {
		return value.getFirstChild( FieldNames.LEVEL.getName() ).strValue();
	}

	public static String message( Value value ) {
		return value.getFirstChild( FieldNames.MESSAGE.getName() ).strValue();
	}

	public static String extendedType( Value value ) {
		return value.getFirstChild( FieldNames.EXTENDED_TYPE.getName() ).strValue();
	}


	public LogEvent( String message, String service, LogLevel logLevel, String processId, String type, String scope,
		ParsingContext context ) {

		super( EventTypes.LOG, service, scope, processId, context, Value.create() );

		data().getFirstChild( FieldNames.LEVEL.getName() ).setValue( logLevel.getLevel() );
		data().getFirstChild( FieldNames.MESSAGE.getName() ).setValue( message );
		data().getFirstChild( FieldNames.EXTENDED_TYPE.getName() ).setValue( type );
	}

	public LogEvent( String message, String service, LogLevel logLevel, String processId, String scope,
		ParsingContext context ) {

		super( EventTypes.LOG, service, scope, processId, context, Value.create() );
		data().getFirstChild( FieldNames.LEVEL.getName() ).setValue( logLevel.getLevel() );
		data().getFirstChild( FieldNames.MESSAGE.getName() ).setValue( message );
	}
}
