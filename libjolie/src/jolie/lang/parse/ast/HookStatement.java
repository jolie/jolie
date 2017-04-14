/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.lang.parse.ast;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

/**
 *
 * @author claudio
 */
public class HookStatement extends OLSyntaxNode
{
	private final VariablePathNode leftPath, rightPath;
	
	public HookStatement( ParsingContext context, VariablePathNode leftPath, VariablePathNode rightPath )
	{
		super( context );
		VariablePathNode.levelPaths( leftPath, rightPath );
		this.leftPath = leftPath;
		this.rightPath = rightPath;
	}
	
	public VariablePathNode leftPath()
	{
		return leftPath;
	}
	
	public VariablePathNode rightPath()
	{
		return rightPath;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
	
}
