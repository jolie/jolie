/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.typing;

import jolie.lang.CodeCheckException;
import jolie.lang.CodeCheckMessage;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.*;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;

//TODO: separate TypeDefinition from Type
public class TypeChecker implements OLVisitor< TypeChecker.Input, TypeDefinition > {
	public static class Env {
	}

	public static class Input {
		private final TypeDefinition type;
		private final Env env;

		private Input( Env env, TypeDefinition type ) {
			this.env = env;
			this.type = type;
		}
	}

	public static class BehaviourType {
	}

	private static class FatalTypeCheckingError extends RuntimeException {
		private final CodeCheckMessage message;
		private FatalTypeCheckingError( CodeCheckMessage message ) {
			this.message = message;
		}
	}

	public static void check( Program program ) throws CodeCheckException {
		try {
			program.accept( new TypeChecker(), new Input() );
		} catch( FatalTypeCheckingError e ) {
			throw new CodeCheckException( e.message );
		}
	}

	public TypeDefinition visit( Program n, TypeChecker.Input input ) {
		n.children().forEach( child -> go( child, input ) );
		return TypeDefinitionUndefined.getInstance();
	}

	public TypeDefinition visit( OneWayOperationDeclaration n, TypeChecker.Input input ) {
		return TypeDefinitionUndefined.getInstance();
	}

	public TypeDefinition visit( RequestResponseOperationDeclaration n, TypeChecker.Input input ) {
		return TypeDefinitionUndefined.getInstance();
	}

	public TypeDefinition visit( SequenceStatement n, TypeChecker.Input input ) {
		TypeChecker.Input currInput = input;
		for( OLSyntaxNode child : n.children() ) {
			TypeDefinition result = go( child, currInput );
			currInput = new Input( input.env, result );
		}
		return currInput.type;
	}

	public TypeDefinition visit( AssignStatement n, TypeChecker.Input input ) {
		// infer type of expression
	}
}
