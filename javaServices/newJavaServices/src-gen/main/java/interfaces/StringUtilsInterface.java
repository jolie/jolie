package interfaces;

import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import types.*;

public interface StringUtilsInterface {
    
    String leftPad( PadRequest request ) throws FaultException;
    
    String valueToPrettyString( JolieValue request ) throws FaultException;
    
    String toLowerCase( String request ) throws FaultException;
    
    Integer length( String request ) throws FaultException;
    
    MatchResult match( MatchRequest request ) throws FaultException;
    
    String urlDecode( UrlDecodeRequest request ) throws FaultException;
    
    String replaceFirst( ReplaceRequest request ) throws FaultException;
    
    StringItemList sort( StringItemList request ) throws FaultException;
    
    /**
     *  Formats a string.
     * 	 For example, a request value "Hello {name}" { name = "Homer" } is transformed into "Hello Homer"
     * 	 You can use formatting rules as in Java's MessageFormat, for example, "Up to {pct,number,percent}" { pct = 0.6 } becomes "Up to 60%"
     * 	
     */
    String fmt( FormatRequest request ) throws FaultException;
    
    String replaceAll( ReplaceRequest request ) throws FaultException;
    
    String urlEncode( UrlEncodeRequest request ) throws FaultException;
    
    String substring( SubStringRequest request ) throws FaultException;
    
    /**
     * 
     * 	 it returns a random UUID
     * 	
     */
    String getRandomUUID() throws FaultException;
    
    String rightPad( PadRequest request ) throws FaultException;
    
    /**
     * 
     * 	  Returns true if the string contains .substring
     * 	 
     */
    Boolean contains( ContainsRequest request ) throws FaultException;
    
    SplitResult split( SplitRequest request ) throws FaultException;
    
    SplitResult splitByLength( SplitByLengthRequest request ) throws FaultException;
    
    String trim( String request ) throws FaultException;
    
    MatchResult find( MatchRequest request ) throws FaultException;
    
    /**
     * 
     * 	  checks if a string ends with a given suffix
     * 	
     */
    Boolean endsWith( EndsWithRequest request ) throws FaultException;
    
    String toUpperCase( String request ) throws FaultException;
    
    String join( JoinRequest request ) throws FaultException;
    
    Integer indexOf( IndexOfRequest request ) throws FaultException;
    
    /**
     * 
     * 	 checks if the passed string starts with a given prefix
     * 	
     */
    Boolean startsWith( StartsWithRequest request ) throws FaultException;
}