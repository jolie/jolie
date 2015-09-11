package jolie.lang.parse.ast.types;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;
import jolie.util.Range;

public class TypeChoiceDefinition extends TypeDefinition
{
	private final TypeDefinition left;
	private final TypeDefinition right;

	public TypeChoiceDefinition( ParsingContext context, String id, Range cardinality, TypeDefinition left, TypeDefinition right )
	{
		super( context, id, cardinality );
		this.left = left;
		this.right = right;
	}

	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}

	public TypeDefinition left()
	{
		return left;
	}

	public TypeDefinition right()
	{
		return right;
	}
	
	@Override
	protected boolean containsPath( Iterator< Pair< OLSyntaxNode, OLSyntaxNode > > it )
	{
		final List< Pair< OLSyntaxNode, OLSyntaxNode > > path = new LinkedList<>();
		it.forEachRemaining( pair -> path.add( pair ) );
		return left.containsPath( path.iterator() ) && right.containsPath( path.iterator() );
	}
}
