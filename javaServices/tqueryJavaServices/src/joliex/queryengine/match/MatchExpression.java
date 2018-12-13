package joliex.queryengine.match;

import java.util.Optional;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Utils;

public interface MatchExpression
{
	Optional<Value> interpret( Value data );
}

abstract class UnaryExpression implements MatchExpression
{
}

final class EqualExp extends UnaryExpression
{
	private CompareExp equal;   //.equal: CompareExp

	EqualExp( CompareExp equal )
	{
		this.equal = equal;
	}

	@Override
	public Optional<Value> interpret( Value data )
	{
		ValueVector values = Utils.evaluatePath( data, equal.getPath() );
		return (values.size() == 1 && values.first().strValue().equals( equal.getValue() )) ? Optional.of( data ) : Optional.empty();
	}
}

final class GreaterThenExp extends UnaryExpression
{
	private CompareExp greaterThen;   //.greaterThen: CompareExp

	GreaterThenExp( CompareExp greaterThen )
	{
		this.greaterThen = greaterThen;
	}

	@Override
	public Optional<Value> interpret( Value data )
	{
		ValueVector values = Utils.evaluatePath( data, greaterThen.getPath() );
		if ( values.size() == 1 ) {
			return Utils.isGreater( values.first(), greaterThen.getValue() ) ? Optional.of( data ) : Optional.empty();
		} else {
			return Optional.empty();
		}
	}
}

final class LowerThenExp extends UnaryExpression
{
	private CompareExp lowerThen;   //.lowerThen: CompareExp

	LowerThenExp( CompareExp lowerThen )
	{
		this.lowerThen = lowerThen;
	}

	@Override
	public Optional<Value> interpret( Value data )
	{
		ValueVector values = Utils.evaluatePath( data, lowerThen.getPath() );
		if ( values.size() == 1 ) {
			return Utils.isGreater( values.first(), lowerThen.getValue() ) ? Optional.empty() : Optional.of( data );
		} else {
			return Optional.empty();
		}
	}
}

final class ExistsExp extends UnaryExpression
{
	private String exists; //.exists: Path

	ExistsExp( String exists )
	{
		this.exists = exists;
	}

	@Override
	public Optional<Value> interpret( Value data )
	{
		return (Utils.evaluatePath( data, exists ).isEmpty())
			? Optional.empty() : Optional.of( data );
	}
}

final class CompareExp
{
	private String path;    //.path: Path
	private String value;   //.value[1,*]: undefined

	CompareExp( String path, String value )
	{
		this.path = path;
		this.value = value;
	}

	String getPath()
	{
		return path;
	}

	String getValue()
	{
		return value;
	}
}

final class OrExp implements MatchExpression
{
	private BinaryExpression or;

	OrExp( BinaryExpression or )
	{
		this.or = or;
	}

	@Override
	public Optional<Value> interpret( Value data )
	{
		return (or.getLeft().interpret( data ).isPresent() || or.getRight().interpret( data ).isPresent())
			? Optional.of( data ) : Optional.empty();
	}
}

final class AndExp implements MatchExpression
{
	private BinaryExpression and;

	AndExp( BinaryExpression and )
	{
		this.and = and;
	}

	@Override
	public Optional<Value> interpret( Value data )
	{
		return (and.getLeft().interpret( data ).isPresent() && and.getRight().interpret( data ).isPresent())
			? Optional.of( data ) : Optional.empty();
	}
}

final class NotExp implements MatchExpression
{
	private MatchExpression not; //.not: BinaryExp

	NotExp( MatchExpression not )
	{
		this.not = not;
	}

	@Override
	public Optional<Value> interpret( Value data )
	{
		return (not.interpret( data ).isPresent())
			? Optional.empty() : Optional.of( data );
	}
}

final class BinaryExpression
{
	private MatchExpression left;   //.left: MatchExp
	private MatchExpression right;  //.right: MatchExp

	public BinaryExpression( MatchExpression left, MatchExpression right )
	{
		this.left = left;
		this.right = right;
	}

	MatchExpression getLeft()
	{
		return left;
	}

	MatchExpression getRight()
	{
		return right;
	}
}
