package joliex.lang;




import jolie.Constants;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.Constants.EmbeddedServiceType;
import jolie.net.CommListener;
import jolie.net.CommMessage;
import jolie.net.LocalCommChannel;
import jolie.net.OutputPort;
import jolie.runtime.EmbeddedServiceLoader;
import jolie.runtime.EmbeddedServiceLoaderCreationException;
import jolie.runtime.EmbeddedServiceLoadingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class RuntimeService extends JavaService
{
	private Interpreter interpreter;
	
	public RuntimeService()
	{
		this.interpreter = Interpreter.getInstance();
	}
	
	public CommMessage getLocalLocation( CommMessage message )
	{
		Value v = Value.create();
		v.setValue( new LocalCommChannel( interpreter ) );
		return new CommMessage( "getLocation", "/", v );
	}
	
	public CommMessage setOutputPort( CommMessage message )
		throws FaultException
	{
		String name = message.value().getFirstChild( "name" ).strValue();
		Value locationValue = message.value().getFirstChild( "location" );
		Value protocolValue = message.value().getFirstChild( "protocol" );
		OutputPort port =
			new OutputPort(
					interpreter(),
					name
				);
		Value l;
		Value r = interpreter.mainThread().state().root();
		l =	r.getFirstChild( name ).getFirstChild( Constants.LOCATION_NODE_NAME );
		if ( locationValue.isChannel() ) {
			l.setValue( locationValue.channelValue() );
		} else {
			l.setValue( locationValue.strValue() );
		}
		r.getFirstChild( name ).getFirstChild( Constants.PROTOCOL_NODE_NAME ).deepCopy( protocolValue );

		r = ExecutionThread.currentThread().state().root();
		l =	r.getFirstChild( name ).getFirstChild( Constants.LOCATION_NODE_NAME );
		if ( locationValue.isChannel() ) {
			l.setValue( locationValue.channelValue() );
		} else {
			l.setValue( locationValue.strValue() );
		}
		r.getFirstChild( name ).getFirstChild( Constants.PROTOCOL_NODE_NAME ).deepCopy( protocolValue );

		interpreter.register( name, port );
		return null;
	}
	
	public CommMessage removeOutputPort( CommMessage message )
	{
		interpreter.removeOutputPort( message.value().strValue() );
		return null;
	}
	
	public CommMessage setRedirection( CommMessage message )
		throws FaultException
	{
		CommMessage ret = null;
		String serviceName = message.value().getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter.commCore().getListenerByInputPortName( serviceName );
		if ( listener == null )
			throw new FaultException( "RuntimeException", "Unknown inputPort: " + serviceName );
		
		String resourceName = message.value().getChildren( "resourceName" ).first().strValue();
		String opName = message.value().getChildren( "outputPortName" ).first().strValue();
		try {
			OutputPort port = interpreter.getOutputPort( opName );
			listener.redirectionMap().put( resourceName, port );
		} catch( InvalidIdException e ) {
			throw new FaultException( "RuntimeException", e );
		}
		
		ret = new CommMessage( "setRedirection", "/", Value.create() );
		return ret;
	}
	
	public CommMessage removeRedirection( CommMessage message )
		throws FaultException
	{
		CommMessage ret = null;
		String serviceName = message.value().getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter.commCore().getListenerByInputPortName( serviceName );
		if ( listener == null )
			throw new FaultException( "RuntimeException", "Unknown inputPort: " + serviceName );
		
		String resourceName = message.value().getChildren( "resourceName" ).first().strValue();
		listener.redirectionMap().remove( resourceName );
		ret = new CommMessage( "removeRedirection", "/", Value.create() );
		return ret;
	}

	public CommMessage getRedirection( CommMessage message )
		throws FaultException
	{
		CommMessage ret = null;
		String inputPortName = message.value().getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter.commCore().getListenerByInputPortName( inputPortName );
		if ( listener == null ) {
			throw new FaultException( "RuntimeException" );
		}
		
		String resourceName = message.value().getChildren( "resourceName" ).first().strValue();
		OutputPort p = listener.redirectionMap().get( resourceName );
		if ( p == null ) {
			ret = new CommMessage( "getRedirection", "/", Value.create() );
		} else {
			ret = new CommMessage( "getRedirection", "/", Value.create( p.id() ) );
		}
		return ret;
	}
	
	public CommMessage loadEmbeddedService( CommMessage message )
		throws FaultException
	{
		CommMessage ret = null;
		try {
			Value channel = Value.create();
			String filePath = message.value().getFirstChild( "filepath" ).strValue();
			String typeStr = message.value().getFirstChild( "type" ).strValue();
			EmbeddedServiceType type =
				jolie.Constants.stringToEmbeddedServiceType( typeStr );
			EmbeddedServiceLoader loader =
				EmbeddedServiceLoader.create( interpreter(), type, filePath, channel );
			loader.load();
			ret = new CommMessage( "loadEmbeddedService", "/", channel );
		} catch( EmbeddedServiceLoaderCreationException e ) {
			e.printStackTrace();
			throw new FaultException( "RuntimeException", e );
		} catch( EmbeddedServiceLoadingException e ) {
			e.printStackTrace();
			throw new FaultException( "RuntimeException", e );
		}
		return ret;
	}
}
