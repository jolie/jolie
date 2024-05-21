package joliex.util.spec.interfaces;

public interface TimeInterface {
    
    /**
     * 
     * 		 Schedules a timeout, which can be cancelled using #cancelTimeout from the returned string. Default .timeunit value is MILLISECONDS, .operation default is "timeout".
     * 		
     */
    java.lang.Long scheduleTimeout( joliex.util.spec.types.ScheduleTimeOutRequest request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		 Converts an input string into a date expressed by means of
     * 		 three elements: day, month and year. The request may specify the
     * 		 date parsing format. See #DateValuesRequestType for details.
     * 		
     */
    joliex.util.spec.types.DateValuesType getDateValues( joliex.util.spec.types.DateValuesRequestType request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		 It returns a date time in a string format starting from a timestamp
     * 		
     */
    joliex.util.spec.types.GetDateTimeResponse getDateTime( joliex.util.spec.types.GetDateTimeRequest request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		 Warning: this is temporary and subject to future change as soon as long is supported by Jolie.
     * 		
     */
    java.lang.Long getCurrentTimeMillis() throws jolie.runtime.FaultException;
    
    java.lang.Integer getDateDiff( joliex.util.spec.types.DiffDateRequestType request ) throws jolie.runtime.FaultException;
    
    /**
     *  It stops the current timeout previously set with a setNextTimeout 
     */
    void stopNextTimeout() throws jolie.runtime.FaultException;
    
    java.lang.Integer getTimeDiff( joliex.util.spec.types.GetTimeDiffRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.Long getTimestampFromString( joliex.util.spec.types.GetTimestampFromStringRequest request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		It Cancels a timeout from a long-value created from #scheduleTimeout
     * 		
     */
    java.lang.Boolean cancelTimeout( java.lang.Long request ) throws jolie.runtime.FaultException;
    
    void setNextTimeoutByTime( jolie.runtime.embedding.java.JolieValue request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		It returns the current date time as a string
     * 		
     */
    java.lang.String getCurrentDateTime( joliex.util.spec.types.CurrentDateTimeRequestType request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		It waits for a period specified in the request (in milliseconds)
     * 		
     */
    void sleep( java.lang.Integer request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		  it sets a timeout whose duration is in milliseconds and it is represented by the root value of the message
     * 		  When the alarm is triggered a message whose content is defined in .message is sent to operation defined in .operation
     * 		  ( default: timeout )
     * 		
     */
    void setNextTimeout( joliex.util.spec.types.SetNextTimeOutRequest request ) throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.TimeValuesType getTimeFromMilliSeconds( java.lang.Integer request ) throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.DateTimeType getDateTimeValues( joliex.util.spec.types.GetTimestampFromStringRequest request ) throws jolie.runtime.FaultException;
    
    void setNextTimeoutByDateTime( jolie.runtime.embedding.java.JolieValue request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 		 Returns the current date split in three fields: day, month and year
     * 		
     */
    joliex.util.spec.types.DateValuesType getCurrentDateValues() throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.TimeValuesType getTimeValues( java.lang.String request ) throws jolie.runtime.FaultException;
}