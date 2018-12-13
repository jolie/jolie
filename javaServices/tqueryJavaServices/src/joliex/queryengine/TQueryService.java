package joliex.queryengine;

import java.util.Optional;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.match.MatchQuery;
import joliex.queryengine.unwind.UnwindQuery;

public class TQueryService extends JavaService
{
	public Value match( Value matchRequest )
	{
		Optional<Value> match = MatchQuery.match( matchRequest );
		return match.orElse( Value.create() );
	}

//	check if it has to return values instead of value vectors
	public ValueVector unwind( Value unwindRequest )
	{
		return UnwindQuery.unwind( unwindRequest );
	}

	public Value project( Value projectRequest )
	{
		return null;
	}

	public Value group( Value groupRequest )
	{
		return null;
	}

	public Value lookup( Value lookupRequest )
	{
		return null;
	}
}
