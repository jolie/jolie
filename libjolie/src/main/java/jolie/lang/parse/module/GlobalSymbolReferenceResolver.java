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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jolie.lang.Constants.OperandType;
import jolie.lang.parse.OLVisitor;
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
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InputPortInfo.AggregationItemInfo;
import jolie.lang.parse.ast.SymbolNode.Privacy;
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
import jolie.lang.parse.ast.SequenceStatement;
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
import jolie.lang.parse.ast.courier.CourierChoiceStatement.InterfaceOneWayBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.InterfaceRequestResponseBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.OperationOneWayBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.OperationRequestResponseBranch;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.AndConditionNode;
import jolie.lang.parse.ast.expression.ConstantBoolExpression;
import jolie.lang.parse.ast.expression.ConstantDoubleExpression;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.ast.expression.ConstantLongExpression;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.FreshValueExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode.Operation;
import jolie.lang.parse.ast.expression.InstanceOfExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.expression.VoidExpressionNode;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.module.ModuleCrawler.ModuleCrawlerResult;
import jolie.lang.parse.module.SymbolInfo.Scope;
import jolie.lang.parse.module.exceptions.DuplicateSymbolException;
import jolie.lang.parse.module.exceptions.IllegalAccessSymbolException;
import jolie.lang.parse.module.exceptions.SymbolNotFoundException;
import jolie.lang.parse.module.exceptions.SymbolTypeMismatchException;
import jolie.util.Helpers;
import jolie.util.Pair;

public class GlobalSymbolReferenceResolver {
	private final Map< URI, ModuleRecord > moduleMap;
	private final Map< URI, SymbolTable > symbolTables;

	public GlobalSymbolReferenceResolver( ModuleCrawlerResult moduleMap ) {
		this.moduleMap = moduleMap.toMap();
		this.symbolTables = new HashMap<>();
		for( ModuleRecord mr : this.moduleMap.values() ) {
			this.symbolTables.put( mr.source(), mr.symbolTable() );
		}
	}

	private class SymbolReferenceResolverVisitor implements OLVisitor {

		private URI currentURI;
		private boolean valid = true;
		private Exception error;

		protected SymbolReferenceResolverVisitor() {}


		/**
		 * Walk through the Jolie AST tree and resolve the call of external Symbols.
		 */
		public void resolve( Program p ) throws ModuleException {
			currentURI = p.context().source();
			visit( p );
			if( !this.valid ) {
				throw new ModuleException( p.context(), this.error );
			}
			return;
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
			for( TypeDefinition fault : decl.faults().values() ) {
				fault.accept( this );
			}
		}

		@Override
		public void visit( DefinitionNode n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( ParallelStatement n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( SequenceStatement n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( NDChoiceStatement n ) {
			for( Pair< OLSyntaxNode, OLSyntaxNode > child : n.children() ) {
				child.key().accept( this );
				child.value().accept( this );
			}
		}

		@Override
		public void visit( OneWayOperationStatement n ) {}

		@Override
		public void visit( RequestResponseOperationStatement n ) {
			n.process().accept( this );
		}

		@Override
		public void visit( NotificationOperationStatement n ) {}

		@Override
		public void visit( SolicitResponseOperationStatement n ) {
			if( n.handlersFunction() != null ) {
				for( Pair< String, OLSyntaxNode > handler : n.handlersFunction().pairs() ) {
					handler.value().accept( this );
				}
			}
		}

		@Override
		public void visit( LinkInStatement n ) {}

		@Override
		public void visit( LinkOutStatement n ) {}

		@Override
		public void visit( AssignStatement n ) {
			n.expression().accept( this );
		}

		@Override
		public void visit( AddAssignStatement n ) {
			n.expression().accept( this );
		}

		@Override
		public void visit( SubtractAssignStatement n ) {
			n.expression().accept( this );
		}

		@Override
		public void visit( MultiplyAssignStatement n ) {
			n.expression().accept( this );
		}

		@Override
		public void visit( DivideAssignStatement n ) {
			n.expression().accept( this );
		}

		@Override
		public void visit( IfStatement n ) {
			for( Pair< OLSyntaxNode, OLSyntaxNode > child : n.children() ) {
				child.key().accept( this );
				child.value().accept( this );
			}
			if( n.elseProcess() != null ) {
				n.elseProcess().accept( this );
			}
		}

		@Override
		public void visit( DefinitionCallStatement n ) {}

		@Override
		public void visit( WhileStatement n ) {
			n.condition().accept( this );
			n.body().accept( this );
		}

		@Override
		public void visit( OrConditionNode n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( AndConditionNode n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( NotExpressionNode n ) {
			n.expression().accept( this );
		}

		@Override
		public void visit( CompareConditionNode n ) {
			n.leftExpression().accept( this );
			n.rightExpression().accept( this );
		}

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
		public void visit( ProductExpressionNode n ) {
			for( Pair< OperandType, OLSyntaxNode > node : n.operands() ) {
				node.value().accept( this );
			}
		}

		@Override
		public void visit( SumExpressionNode n ) {
			for( Pair< OperandType, OLSyntaxNode > node : n.operands() ) {
				node.value().accept( this );
			}
		}

		@Override
		public void visit( VariableExpressionNode n ) {}

		@Override
		public void visit( NullProcessStatement n ) {}

		@Override
		public void visit( jolie.lang.parse.ast.Scope n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( InstallStatement n ) {
			for( Pair< String, OLSyntaxNode > handlerFunction : n.handlersFunction().pairs() ) {
				handlerFunction.value().accept( this );
			}
		}

		@Override
		public void visit( CompensateStatement n ) {}

		@Override
		public void visit( ThrowStatement n ) {
			if( n.expression() != null ) {
				n.expression().accept( this );
			}
		}

		@Override
		public void visit( ExitStatement n ) {}

		@Override
		public void visit( ExecutionInfo n ) {}

		@Override
		public void visit( CorrelationSetInfo n ) {}

		@Override
		public void visit( InputPortInfo n ) {
			// resolve interface definition
			for( InterfaceDefinition iface : n.getInterfaceList() ) {
				Optional< SymbolInfo > symbol = getSymbol( iface.context(), iface.name() );
				if( !symbol.isPresent() ) {
					this.valid = false;
					this.error = new SymbolNotFoundException( iface.name() );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					this.valid = false;
					this.error = new SymbolTypeMismatchException( n.id(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() );
					return;
				}
				InterfaceDefinition ifaceDeclFromSymbol = (InterfaceDefinition) symbol.get().node();
				ifaceDeclFromSymbol.operationsMap().values().forEach( op -> {
					iface.addOperation( op );
					n.addOperation( op );
				} );
				iface.setDocumentation( ifaceDeclFromSymbol.getDocumentation() );
			}
			for( OperationDeclaration op : n.operations() ) {
				op.accept( this );
			}
			for( AggregationItemInfo aggregationItem : n.aggregationList() ) {
				if( aggregationItem.interfaceExtender() != null ) {
					aggregationItem.interfaceExtender().accept( this );
				}
			}
		}

		@Override
		public void visit( OutputPortInfo n ) {
			// resolve interface definition
			for( InterfaceDefinition iface : n.getInterfaceList() ) {
				Optional< SymbolInfo > symbol = getSymbol( iface.context(), iface.name() );
				if( !symbol.isPresent() ) {
					this.valid = false;
					this.error = new SymbolNotFoundException( iface.name() );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					this.valid = false;
					this.error = new SymbolTypeMismatchException( iface.name(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() );
					return;
				}
				InterfaceDefinition ifaceDeclFromSymbol = (InterfaceDefinition) symbol.get().node();
				ifaceDeclFromSymbol.operationsMap().values().forEach( op -> {
					iface.addOperation( op );
					n.addOperation( op );
				} );
				iface.setDocumentation( ifaceDeclFromSymbol.getDocumentation() );
			}
			for( OperationDeclaration op : n.operations() ) {
				op.accept( this );
			}
		}

		@Override
		public void visit( PointerStatement n ) {}

		@Override
		public void visit( DeepCopyStatement n ) {
			n.rightExpression().accept( this );
		}

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
		public void visit( ForStatement n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( ForEachSubNodeStatement n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( ForEachArrayItemStatement n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( SpawnStatement n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( IsTypeExpressionNode n ) {}

		@Override
		public void visit( InstanceOfExpressionNode n ) {
			n.type().accept( this );
		}

		@Override
		public void visit( TypeCastExpressionNode n ) {}

		@Override
		public void visit( SynchronizedStatement n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( CurrentHandlerStatement n ) {}

		@Override
		public void visit( EmbeddedServiceNode n ) {
			if( n.program() != null ) {
				n.program().accept( this );
			}
		}


		@Override
		public void visit( InstallFixedVariableExpressionNode n ) {}

		@Override
		public void visit( VariablePathNode n ) {}

		@Override
		public void visit( TypeInlineDefinition n ) {
			if( n.hasSubTypes() ) {
				for( Map.Entry< String, TypeDefinition > subType : n.subTypes() ) {
					subType.getValue().accept( this );
				}
			}
		}

		@Override
		public void visit( TypeDefinitionLink n ) {
			TypeDefinition linkedType = null;
			if( n.linkedTypeName().equals( TypeDefinitionUndefined.UNDEFINED_KEYWORD ) ) {
				linkedType = TypeDefinitionUndefined.getInstance();
			} else {
				Optional< SymbolInfo > targetSymbolInfo =
					getSymbol( n.context(), n.linkedTypeName() );
				if( !targetSymbolInfo.isPresent() ) {
					this.valid = false;
					this.error = new SymbolNotFoundException( n.id() );
					return;
				}
				if( !(targetSymbolInfo.get().node() instanceof TypeDefinition) ) {
					this.valid = false;
					this.error = new SymbolTypeMismatchException( n.id(), "TypeDefinition",
						targetSymbolInfo.get().node().getClass().getSimpleName() );
					return;
				}
				linkedType = (TypeDefinition) targetSymbolInfo.get().node();
				if( linkedType.equals( n ) ) {
					this.valid = false;
					this.error = new SymbolNotFoundException( n.id() );
					return;
				}
			}
			n.setLinkedType( linkedType );
		}

		@Override
		public void visit( InterfaceDefinition n ) {
			for( OperationDeclaration op : n.operationsMap().values() ) {
				op.accept( this );
			}
		}

		@Override
		public void visit( DocumentationComment n ) {}

		@Override
		public void visit( FreshValueExpressionNode n ) {}

		@Override
		public void visit( CourierDefinitionNode n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( CourierChoiceStatement n ) {
			for( InterfaceOneWayBranch owIfaceBranch : n.interfaceOneWayBranches() ) {
				InterfaceDefinition iface = owIfaceBranch.interfaceDefinition;

				Optional< SymbolInfo > symbol = getSymbol( iface.context(), iface.name() );
				if( !symbol.isPresent() ) {
					this.valid = false;
					this.error = new SymbolNotFoundException( iface.name() );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					this.valid = false;
					this.error = new SymbolTypeMismatchException( iface.name(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() );
					return;
				}
				InterfaceDefinition ifaceDeclFromSymbol = (InterfaceDefinition) symbol.get().node();
				ifaceDeclFromSymbol.operationsMap().values().forEach( op -> {
					iface.addOperation( op );
					op.accept( this );
				} );
				iface.setDocumentation( ifaceDeclFromSymbol.getDocumentation() );

				owIfaceBranch.body.accept( this );
			}

			for( InterfaceRequestResponseBranch rrIfaceBranch : n
				.interfaceRequestResponseBranches() ) {
				InterfaceDefinition iface = rrIfaceBranch.interfaceDefinition;

				Optional< SymbolInfo > symbol = getSymbol( iface.context(), iface.name() );
				if( !symbol.isPresent() ) {
					this.valid = false;
					this.error = new SymbolNotFoundException( iface.name() );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					this.valid = false;
					this.error = new SymbolTypeMismatchException( iface.name(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() );
					return;
				}
				InterfaceDefinition ifaceDeclFromSymbol = (InterfaceDefinition) symbol.get().node();
				ifaceDeclFromSymbol.operationsMap().values().forEach( op -> {
					iface.addOperation( op );
					op.accept( this );
				} );
				iface.setDocumentation( ifaceDeclFromSymbol.getDocumentation() );

				rrIfaceBranch.body.accept( this );
			}

			for( OperationOneWayBranch owBranch : n.operationOneWayBranches() ) {
				owBranch.body.accept( this );
			}

			for( OperationRequestResponseBranch rrBranch : n.operationRequestResponseBranches() ) {
				rrBranch.body.accept( this );
			}
		}

		@Override
		public void visit( NotificationForwardStatement n ) {}

		@Override
		public void visit( SolicitResponseForwardStatement n ) {}

		@Override
		public void visit( InterfaceExtenderDefinition n ) {
			if( n.defaultOneWayOperation() != null ) {
				n.defaultOneWayOperation().accept( this );
			}
			if( n.defaultRequestResponseOperation() != null ) {
				n.defaultRequestResponseOperation().accept( this );
			}
			for( OperationDeclaration op : n.operationsMap().values() ) {
				op.accept( this );
			}
		}

		@Override
		public void visit( InlineTreeExpressionNode n ) {
			for( Operation operation : n.operations() ) {
				if( operation instanceof InlineTreeExpressionNode.AssignmentOperation ) {
					((InlineTreeExpressionNode.AssignmentOperation) operation).expression()
						.accept( this );;
				} else if( operation instanceof InlineTreeExpressionNode.DeepCopyOperation ) {
					((InlineTreeExpressionNode.DeepCopyOperation) operation).expression()
						.accept( this );;
				}
			}
		}

		@Override
		public void visit( VoidExpressionNode n ) {}

		@Override
		public void visit( ProvideUntilStatement n ) {
			n.provide().accept( this );
			n.until().accept( this );
		}

		@Override
		public void visit( TypeChoiceDefinition n ) {
			n.left().accept( this );
			if( n.right() != null ) {
				n.right().accept( this );
			}
		}

		@Override
		public void visit( ImportStatement n ) {}

		private Optional< SymbolInfo > getSymbol( ParsingContext context, String name ) {
			Optional< SymbolInfo > symbol = Helpers.firstNonNull( () -> {
				if( !symbolTables.containsKey( currentURI )
					|| !symbolTables.get( currentURI ).symbol( name ).isPresent() ) {
					return null;
				}
				return symbolTables.get( currentURI ).symbol( name ).get();
			}, () -> {
				if( !symbolTables.containsKey( context.source() )
					|| !symbolTables.get( context.source() ).symbol( name ).isPresent() ) {
					return null;
				}
				return symbolTables.get( context.source() ).symbol( name ).get();
			} );
			return symbol;
		}
	}

	/**
	 * perform lookup to external symbol's source of AST node
	 * 
	 * @throws SymbolNotFoundException
	 */
	private SymbolInfo symbolSourceLookup( SymbolInfoExternal symbolInfo )
		throws SymbolNotFoundException {
		ModuleRecord externalSourceRecord =
			this.moduleMap.get( symbolInfo.moduleSource().get().source() );
		Optional< SymbolInfo > externalSourceSymbol =
			externalSourceRecord.symbol( symbolInfo.moduleSymbol() );
		if( !externalSourceSymbol.isPresent() ) {
			throw new SymbolNotFoundException( symbolInfo.name(), symbolInfo.moduleTargets() );
		}
		if( externalSourceSymbol.get().scope() == Scope.LOCAL ) {
			return externalSourceSymbol.get();
		} else {
			return symbolSourceLookup( (SymbolInfoExternal) externalSourceSymbol.get() );
		}
	}

	/**
	 * Find and set a pointer of externalSymbol to it's corresponding AST node by perform lookup at
	 * ModuleRecord Map, a result from ModuleCrawler.
	 * 
	 * @throws DuplicateSymbolException
	 * @throws IllegalAccessSymbolException
	 * @throws SymbolNotFoundException
	 * 
	 */
	public void resolveExternalSymbols()
		throws SymbolNotFoundException, IllegalAccessSymbolException, DuplicateSymbolException {
		for( ModuleRecord md : moduleMap.values() ) {
			for( SymbolInfoExternal localSymbol : md.externalSymbols() ) {
				if( localSymbol instanceof SymbolWildCard ) {
					ModuleRecord wildcardImportedRecord =
						this.moduleMap.get( localSymbol.moduleSource().get().source() );
					md.addWildcardImportedRecord( (SymbolWildCard) localSymbol,
						wildcardImportedRecord.symbols() );
				} else {
					SymbolInfo targetSymbol = symbolSourceLookup( localSymbol );
					if( targetSymbol.privacy() == Privacy.PRIVATE ) {
						throw new IllegalAccessSymbolException( localSymbol.name(),
							localSymbol.moduleTargets() );
					}
					localSymbol.setPointer( targetSymbol.node() );
				}
			}
		}
	}

	/**
	 * resolve LinkedType of each ModuleRecord AST node in the Map.
	 * 
	 * @throws ModuleException if the linked type cannot find it's referencing node
	 */
	public void resolveLinkedType() throws ModuleException {
		SymbolReferenceResolverVisitor resolver = new SymbolReferenceResolverVisitor();
		for( ModuleRecord md : moduleMap.values() ) {
			resolver.resolve( md.program() );
		}
	}

	/**
	 * Resolve symbols the is imported from external modules and resolve linked type's pointer
	 * 
	 * @throws ModuleException if the process is failed
	 */
	public void resolve() throws ModuleException {
		try {
			this.resolveExternalSymbols();
		} catch( SymbolNotFoundException | IllegalAccessSymbolException
			| DuplicateSymbolException e ) {
			throw new ModuleException( e );
		}
		this.resolveLinkedType();
	}

	public Map< URI, SymbolTable > symbolTables() {
		return this.symbolTables;
	}
}
