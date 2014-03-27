using Jolie.runtime;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Jolie.net.protocols
{
    public abstract class CommProtocol
{
    //private readonly static class LazyDummyChannelHolder {
    //    private LazyDummyChannelHolder() {}
    //    private static class DummyChannel extends AbstractCommChannel {
    //        public void closeImpl() {}
    //        public void sendImpl( CommMessage message ) {}
    //        public CommMessage recvImpl() { return CommMessage.UNDEFINED_MESSAGE; }
    //    }

    //    private static DummyChannel dummyChannel = new DummyChannel();
    //}

    //private static class Parameters {
    //    public static readonly String OPERATION_SPECIFIC_CONFIGURATION = "osc";
    //}


    //private readonly VariablePath configurationPath;
    //private CommChannel channel = null;

    //protected VariablePath configurationPath()
    //{
    //    return configurationPath;
    //}

    //public abstract String name();
	
    //public CommProtocol( VariablePath configurationPath )
    //{
    //    this.configurationPath = configurationPath;
    //}
	
    //public void setChannel( CommChannel channel )
    //{
    //    this.channel = channel;
    //}

    //protected CommChannel channel()
    //{
    //    if ( this.channel == null ) {
    //        return LazyDummyChannelHolder.dummyChannel;
    //    }
    //    return this.channel;
    //}
	
    //protected ValueVector getParameterVector( String id )
    //{
    //    return configurationPath.getValue().getChildren( id );
    //}
	
    //protected Boolean hasParameter( String id )
    //{
    //    if ( configurationPath.getValue().hasChildren( id ) ) {
    //        Value v = configurationPath.getValue().getFirstChild( id );
    //        return v.IsDefined || v.HasChildren;
    //    }
    //    return false;
    //}
	
    ///**
    // * Shortcut for getParameterVector( id ).first()
    // */
    //protected Value getParameterFirstValue( String id )
    //{
    //    return getParameterVector( id ).First();
    //}
	
    ///**
    // * Shortcut for checking if a parameter intValue() equals 1
    // * @param id the parameter identifier
    // */
    //protected Boolean checkBooleanParameter( String id )
    //{
    //    return hasParameter( id ) && getParameterFirstValue( id ).BoolValue;
    //}

    ///**
    // * Shortcut for checking if a parameter intValue() equals 1
    // * @param id the parameter identifier
    // */
    //protected Boolean checkBooleanParameter( String id, Boolean defaultValue )
    //{
    //    if ( hasParameter( id ) ) {
    //        return getParameterFirstValue( id ).BoolValue;
    //    } else {
    //        return defaultValue;
    //    }
    //}
	
    ///**
    // * Shortcut for <code>getParameterFirstValue( id ).strValue()</code>
    // * @param id the parameter identifier
    // */
    //protected String getStringParameter( String id )
    //{
    //    return ( hasParameter( id ) ? getParameterFirstValue( id ).StrValue : "" );
    //}

    //protected Boolean hasOperationSpecificParameter( String operationName, String parameterName )
    //{
    //    if ( hasParameter( Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
    //        Value osc = getParameterFirstValue( Parameters.OPERATION_SPECIFIC_CONFIGURATION );
    //        if ( osc.hasChildren( operationName ) ) {
    //            return osc.GetFirstChild( operationName ).hasChildren( parameterName );
    //        }
    //    }
    //    return false;
    //}

    //protected String getOperationSpecificStringParameter( String operationName, String parameterName )
    //{
    //    if ( hasParameter( Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
    //        Value osc = getParameterFirstValue( Parameters.OPERATION_SPECIFIC_CONFIGURATION );
    //        if ( osc.hasChildren( operationName ) ) {
    //            Value opConfig = osc.GetFirstChild( operationName );
    //            if ( opConfig.hasChildren( parameterName ) ) {
    //                return opConfig.GetFirstChild( parameterName ).StrValue;
    //            }
    //        }
    //    }
    //    return "";
    //}

    ///**
    // * Shortcut for getOperationSpecificParameterVector( id ).first()
    // */
    //protected Value getOperationSpecificParameterFirstValue( String operationName, String parameterName )
    //{
    //    return getOperationSpecificParameterVector( operationName, parameterName ).First();
    //}

    //protected ValueVector getOperationSpecificParameterVector( String operationName, String parameterName )
    //{
    //    Value osc = getParameterFirstValue( Parameters.OPERATION_SPECIFIC_CONFIGURATION );
    //    return osc.GetFirstChild( operationName ).GetChildren( parameterName );
    //}

    ///**
    // * Shortcut for <code>getParameterFirstValue( id ).intValue()</code>
    // * @param id the parameter identifier
    // */
    //protected int getIntParameter( String id )
    //{
    //    return hasParameter( id ) ? getParameterFirstValue( id ).IntValue : 0;
    //}
	
    //abstract public CommMessage recv( StreamReader istream, StreamWriter ostream ); //throws IOException;

    //abstract public void send(StreamWriter ostream, CommMessage message, StreamReader istream); //throws IOException;

    //abstract public Boolean isThreadSafe();
}

}
