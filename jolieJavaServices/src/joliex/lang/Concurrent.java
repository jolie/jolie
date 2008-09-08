package joliex.lang;


import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;

public class Concurrent
{
	public CommMessage notify( CommMessage message )
		throws FaultException
	{
		Interpreter.getInstance().getLock(
					message.value().strValue()
				).notify();
		return CommMessage.createEmptyMessage();
	}
	
	public CommMessage notifyAll( CommMessage message )
		throws FaultException
	{
		Interpreter.getInstance().getLock(
					message.value().strValue()
				).notifyAll();
		return CommMessage.createEmptyMessage();
	}
	
	public CommMessage wait( CommMessage message )
		throws FaultException
	{
		try {
			Interpreter.getInstance().getLock(
					message.value().strValue()
				).wait();
		} catch( InterruptedException ie ) {}
		return CommMessage.createEmptyMessage();
	}
}
