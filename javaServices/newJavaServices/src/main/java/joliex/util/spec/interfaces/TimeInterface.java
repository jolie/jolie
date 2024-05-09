package joliex.util.spec.interfaces;

import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import joliex.util.spec.types.*;

public interface TimeInterface {
    
    /**
     * 
     * 		 Schedules a timeout, which can be cancelled using #cancelTimeout from the returned string. Default .timeunit value is MILLISECONDS, .operation default is "timeout".
     * 		
     */
    Long scheduleTimeout( ScheduleTimeOutRequest request ) throws FaultException;
    
    /**
     * 
     * 		 Converts an input string into a date expressed by means of
     * 		 three elements: day, month and year. The request may specify the
     * 		 date parsing format. See #DateValuesRequestType for details.
     * 		
     */
    DateValuesType getDateValues( DateValuesRequestType request ) throws FaultException;
    
    /**
     * 
     * 		 It returns a date time in a string format starting from a timestamp
     * 		
     */
    GetDateTimeResponse getDateTime( GetDateTimeRequest request ) throws FaultException;
    
    /**
     * 
     * 		 Warning: this is temporary and subject to future change as soon as long is supported by Jolie.
     * 		
     */
    Long getCurrentTimeMillis() throws FaultException;
    
    Integer getDateDiff( DiffDateRequestType request ) throws FaultException;
    
    /**
     *  It stops the current timeout previously set with a setNextTimeout 
     */
    void stopNextTimeout() throws FaultException;
    
    Integer getTimeDiff( GetTimeDiffRequest request ) throws FaultException;
    
    Long getTimestampFromString( GetTimestampFromStringRequest request ) throws FaultException;
    
    /**
     * 
     * 		It Cancels a timeout from a long-value created from #scheduleTimeout
     * 		
     */
    Boolean cancelTimeout( Long request ) throws FaultException;
    
    void setNextTimeoutByTime( JolieValue request ) throws FaultException;
    
    /**
     * 
     * 		It returns the current date time as a string
     * 		
     */
    String getCurrentDateTime( CurrentDateTimeRequestType request ) throws FaultException;
    
    /**
     * 
     * 		It waits for a period specified in the request (in milliseconds)
     * 		
     */
    void sleep( Integer request ) throws FaultException;
    
    /**
     * 
     * 		  it sets a timeout whose duration is in milliseconds and it is represented by the root value of the message
     * 		  When the alarm is triggered a message whose content is defined in .message is sent to operation defined in .operation
     * 		  ( default: timeout )
     * 		
     */
    void setNextTimeout( SetNextTimeOutRequest request ) throws FaultException;
    
    TimeValuesType getTimeFromMilliSeconds( Integer request ) throws FaultException;
    
    DateTimeType getDateTimeValues( GetTimestampFromStringRequest request ) throws FaultException;
    
    void setNextTimeoutByDateTime( JolieValue request ) throws FaultException;
    
    /**
     * 
     * 		 Returns the current date split in three fields: day, month and year
     * 		
     */
    DateValuesType getCurrentDateValues() throws FaultException;
    
    TimeValuesType getTimeValues( String request ) throws FaultException;
}