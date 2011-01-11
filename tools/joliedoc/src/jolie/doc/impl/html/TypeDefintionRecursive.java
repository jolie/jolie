/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.doc.impl.html;

import jolie.lang.parse.ast.types.TypeDefinition;

/**
 *
 * @author balint Maschio
 */
public class TypeDefintionRecursive
{
	private TypeDefinition type;

	public TypeDefintionRecursive( TypeDefinition type )
	{
		this.type = type;


	}

	public TypeDefinition getType()
	{

		return type;

	}

	@Override

	public boolean equals( Object object )
	{
		if ( ((TypeDefinition)object).id().equals( this.type.id() ) ) {

			return true;
		} else {

			return false;

		}


	}
}
