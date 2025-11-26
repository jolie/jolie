/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
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

package jolie.lang.parse.module;

import java.util.Map;

import jolie.lang.CodeCheckMessage;
import jolie.lang.NativeType;
import jolie.lang.parse.UnitOLVisitor;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.EmbedServiceNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.ImportSymbolTarget;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ProvideUntilStatement;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.module.exceptions.DuplicateSymbolException;

public class SymbolTableGenerator {

	private static class SymbolTableGeneratorVisitor implements UnitOLVisitor {
		private final SymbolTable symbolTable;
		private boolean valid = true;
		private ModuleException error;


		protected SymbolTableGeneratorVisitor( ParsingContext context ) {
			this.symbolTable = new SymbolTable( context.source() );
		}

		public SymbolTable generate( Program p ) throws ModuleException {
			visit( p );
			if( !this.valid ) {
				throw error;
			}
			return this.symbolTable;
		}


		@Override
		public void visit( Program n ) {
			for( OLSyntaxNode node : n.children() ) {
				if( !this.valid ) {
					return;
				}
				node.accept( this );
			}
		}

		@Override
		public void visit( OneWayOperationDeclaration decl ) {
			decl.requestType().accept( this );
		}

		@Override
		public void visit( RequestResponseOperationDeclaration decl ) {
			decl.requestType().accept( this );
			decl.responseType().accept( this );
			for( Map.Entry< String, TypeDefinition > fault : decl.faults().entrySet() ) {
				fault.getValue().accept( this );
			}
		}

		@Override
		public void visit( DefinitionNode n ) {}

		@Override
		public void visit( ParallelStatement n ) {}

		@Override
		public void visit( SequenceStatement n ) {}

		@Override
		public void visit( NDChoiceStatement n ) {}

		@Override
		public void visit( OneWayOperationStatement n ) {}

		@Override
		public void visit( RequestResponseOperationStatement n ) {}

		@Override
		public void visit( NotificationOperationStatement n ) {}

		@Override
		public void visit( SolicitResponseOperationStatement n ) {}

		@Override
		public void visit( LinkInStatement n ) {}

		@Override
		public void visit( LinkOutStatement n ) {}

		@Override
		public void visit( AssignStatement n ) {}

		@Override
		public void visit( AddAssignStatement n ) {}

		@Override
		public void visit( SubtractAssignStatement n ) {}

		@Override
		public void visit( MultiplyAssignStatement n ) {}

		@Override
		public void visit( DivideAssignStatement n ) {}

		@Override
		public void visit( IfStatement n ) {}

		@Override
		public void visit( DefinitionCallStatement n ) {}

		@Override
		public void visit( WhileStatement n ) {}

		@Override
		public void visit( OrConditionNode n ) {}

		@Override
		public void visit( AndConditionNode n ) {}

		@Override
		public void visit( NotExpressionNode n ) {}

		@Override
		public void visit( CompareConditionNode n ) {}

		@Override
		public void visit( ConstantIntegerExpression n ) {}

		@Override
		public void visit( ConstantDoubleExpression n ) {}

		@Override
		public void visit( ConstantBoolExpression n ) {}

		@Override
		public void visit( ConstantLongExpression n ) {}

		@Override
		public void visit( ConstantStringExpression n ) {}

		@Override
		public void visit( CurrentValueNode n ) {}

		@Override
		public void visit( PathsExpressionNode n ) {}

		@Override
		public void visit( ValuesExpressionNode n ) {}

		@Override
		public void visit( PvalExpressionNode n ) {}

		@Override
		public void visit( ProductExpressionNode n ) {}

		@Override
		public void visit( SumExpressionNode n ) {}

		@Override
		public void visit( VariableExpressionNode n ) {}

		@Override
		public void visit( NullProcessStatement n ) {}

		@Override
		public void visit( Scope n ) {}

		@Override
		public void visit( InstallStatement n ) {}

		@Override
		public void visit( CompensateStatement n ) {}

		@Override
		public void visit( ThrowStatement n ) {}

		@Override
		public void visit( ExitStatement n ) {}

		@Override
		public void visit( ExecutionInfo n ) {}

		@Override
		public void visit( CorrelationSetInfo n ) {}

		@Override
		public void visit( InputPortInfo n ) {}

		@Override
		public void visit( OutputPortInfo n ) {}

		@Override
		public void visit( PointerStatement n ) {}

		@Override
		public void visit( DeepCopyStatement n ) {}

		@Override
		public void visit( RunStatement n ) {}

		@Override
		public void visit( UndefStatement n ) {}

		@Override
		public void visit( ValueVectorSizeExpressionNode n ) {}

		@Override
		public void visit( PreIncrementStatement n ) {}

		@Override
		public void visit( PostIncrementStatement n ) {}

		@Override
		public void visit( PreDecrementStatement n ) {}

		@Override
		public void visit( PostDecrementStatement n ) {}

		@Override
		public void visit( ForStatement n ) {}

		@Override
		public void visit( ForEachSubNodeStatement n ) {}

		@Override
		public void visit( ForEachArrayItemStatement n ) {}

		@Override
		public void visit( SpawnStatement n ) {}

		@Override
		public void visit( IsTypeExpressionNode n ) {}

		@Override
		public void visit( InstanceOfExpressionNode n ) {}

		@Override
		public void visit( TypeCastExpressionNode n ) {}

		@Override
		public void visit( SynchronizedStatement n ) {}

		@Override
		public void visit( CurrentHandlerStatement n ) {}

		@Override
		public void visit( EmbeddedServiceNode n ) {}

		@Override
		public void visit( InstallFixedVariableExpressionNode n ) {}

		@Override
		public void visit( VariablePathNode n ) {}

		@Override
		public void visit( TypeInlineDefinition n ) {
			if( NativeType.isNativeTypeKeyword( n.name() ) ) {
				return;
			}
			try {
				this.symbolTable.addSymbol( n.name(), n );
			} catch( DuplicateSymbolException e ) {
				this.valid = false;
				this.error = new ModuleException( CodeCheckMessage.withoutHelp( n.context(), e.toString() ) );
			}
		}

		@Override
		public void visit( TypeDefinitionLink n ) {
			try {
				if( !this.symbolTable.getSymbol( n.name() ).isPresent() ) {
					this.symbolTable.addSymbol( n.name(), n );
				}
			} catch( DuplicateSymbolException e ) {
				this.valid = false;
				this.error = new ModuleException( CodeCheckMessage.withoutHelp( n.context(), e.toString() ) );
			}
		}

		@Override
		public void visit( IfExpressionNode n ) {
			n.guard().accept( this );
			n.thenExpression().accept( this );
			n.elseExpression().accept( this );
		}

		@Override
		public void visit( InterfaceDefinition n ) {
			try {
				this.symbolTable.addSymbol( n.name(), n );
			} catch( DuplicateSymbolException e ) {
				this.valid = false;
				this.error = new ModuleException( CodeCheckMessage.withoutHelp( n.context(), e.toString() ) );
				return;
			}
			for( Map.Entry< String, OperationDeclaration > op : n.operationsMap().entrySet() ) {
				op.getValue().accept( this );
			}
		}

		@Override
		public void visit( DocumentationComment n ) {}

		@Override
		public void visit( FreshValueExpressionNode n ) {}

		@Override
		public void visit( CourierDefinitionNode n ) {}

		@Override
		public void visit( CourierChoiceStatement n ) {}

		@Override
		public void visit( NotificationForwardStatement n ) {}

		@Override
		public void visit( SolicitResponseForwardStatement n ) {}

		@Override
		public void visit( InterfaceExtenderDefinition n ) {
			try {
				this.symbolTable.addSymbol( n.name(), n );
			} catch( DuplicateSymbolException e ) {
				this.valid = false;
				this.error = new ModuleException( CodeCheckMessage.withoutHelp( n.context(), e.toString() ) );
			}
		}

		@Override
		public void visit( InlineTreeExpressionNode n ) {}

		@Override
		public void visit( VoidExpressionNode n ) {}

		@Override
		public void visit( ProvideUntilStatement n ) {}

		@Override
		public void visit( TypeChoiceDefinition n ) {
			try {
				this.symbolTable.addSymbol( n.name(), n );
			} catch( DuplicateSymbolException e ) {
				this.valid = false;
				this.error = new ModuleException( CodeCheckMessage.withoutHelp( n.context(), e.toString() ) );
			}
		}

		@Override
		public void visit( ImportStatement n ) {
			ImportPath importPath = new ImportPath( n.importTarget() );
			if( n.isNamespaceImport() ) {
				this.symbolTable.addWildcardSymbol( n.context(), importPath );
			} else {
				for( ImportSymbolTarget targetSymbol : n.importSymbolTargets() ) {
					try {
						this.symbolTable.addSymbolWithAlias( n.context(), targetSymbol.localSymbolName(),
							importPath, targetSymbol.originalSymbolName() );
					} catch( DuplicateSymbolException e ) {
						this.valid = false;
						this.error = new ModuleException( CodeCheckMessage.withoutHelp( n.context(), e.toString() ) );
					}
				}
			}
		}

		@Override
		public void visit( ServiceNode n ) {
			try {
				this.symbolTable.addSymbol( n.name(), n );
			} catch( DuplicateSymbolException e ) {
				this.valid = false;
				this.error = new ModuleException( CodeCheckMessage.withoutHelp( n.context(), e.toString() ) );
			}
		}

		@Override
		public void visit( EmbedServiceNode n ) {}

		@Override
		public void visit( SolicitResponseExpressionNode n ) {}

	}

	/**
	 * generate a SymbolTable of a Jolie AST. As the current implementation, it is walk through the
	 * Jolie's program and read the definition of types and interfaces and create a SymbolInfo node for
	 * the AST. The import statement is consumed here to create an external SymbolInfo to be resolve
	 * later by SymbolReferenceResolver class
	 *
	 * @param program a Jolie AST
	 * @throws ModuleException when the duplication of SymbolDeclaration is detected.
	 */
	public static SymbolTable generate( Program program ) throws ModuleException {
		return (new SymbolTableGeneratorVisitor( program.context() )).generate( program );
	}

}
