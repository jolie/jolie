package joliex.util.spec.interfaces;

public interface StringUtilsInterface {
    
    java.lang.String leftPad( joliex.util.spec.types.PadRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String valueToPrettyString( jolie.runtime.embedding.java.JolieValue request ) throws jolie.runtime.FaultException;
    
    java.lang.String toLowerCase( java.lang.String request ) throws jolie.runtime.FaultException;
    
    java.lang.Integer length( java.lang.String request ) throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.MatchResult match( joliex.util.spec.types.MatchRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String urlDecode( joliex.util.spec.types.UrlDecodeRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String replaceFirst( joliex.util.spec.types.ReplaceRequest request ) throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.StringItemList sort( joliex.util.spec.types.StringItemList request ) throws jolie.runtime.FaultException;
    
    /**
     *  Formats a string.
     * 	 For example, a request value "Hello {name}" { name = "Homer" } is transformed into "Hello Homer"
     * 	 You can use formatting rules as in Java's MessageFormat, for example, "Up to {pct,number,percent}" { pct = 0.6 } becomes "Up to 60%"
     * 	
     */
    java.lang.String fmt( joliex.util.spec.types.FormatRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String replaceAll( joliex.util.spec.types.ReplaceRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String urlEncode( joliex.util.spec.types.UrlEncodeRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String substring( joliex.util.spec.types.SubStringRequest request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 	 it returns a random UUID
     * 	
     */
    java.lang.String getRandomUUID() throws jolie.runtime.FaultException;
    
    java.lang.String rightPad( joliex.util.spec.types.PadRequest request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 	  Returns true if the string contains .substring
     * 	 
     */
    java.lang.Boolean contains( joliex.util.spec.types.ContainsRequest request ) throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.SplitResult split( joliex.util.spec.types.SplitRequest request ) throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.SplitResult splitByLength( joliex.util.spec.types.SplitByLengthRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String trim( java.lang.String request ) throws jolie.runtime.FaultException;
    
    joliex.util.spec.types.MatchResult find( joliex.util.spec.types.MatchRequest request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 	  checks if a string ends with a given suffix
     * 	
     */
    java.lang.Boolean endsWith( joliex.util.spec.types.EndsWithRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.String toUpperCase( java.lang.String request ) throws jolie.runtime.FaultException;
    
    java.lang.String join( joliex.util.spec.types.JoinRequest request ) throws jolie.runtime.FaultException;
    
    java.lang.Integer indexOf( joliex.util.spec.types.IndexOfRequest request ) throws jolie.runtime.FaultException;
    
    /**
     * 
     * 	 checks if the passed string starts with a given prefix
     * 	
     */
    java.lang.Boolean startsWith( joliex.util.spec.types.StartsWithRequest request ) throws jolie.runtime.FaultException;
}