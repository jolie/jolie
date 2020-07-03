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

import jolie.lang.CodeCheckingError;
import jolie.lang.Constants.OperandType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.*;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationAliasInfo;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationVariableInfo;
import jolie.lang.parse.ast.ImportableSymbol.AccessModifier;
import jolie.lang.parse.ast.InputPortInfo.AggregationItemInfo;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.InterfaceOneWayBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.InterfaceRequestResponseBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.OperationOneWayBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.OperationRequestResponseBranch;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode.Operation;
import jolie.lang.parse.ast.types.*;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.module.ModuleCrawler.CrawlerResult;
import jolie.lang.parse.module.SymbolInfo.Scope;
import jolie.lang.parse.module.exceptions.DuplicateSymbolException;
import jolie.lang.parse.module.exceptions.IllegalAccessSymbolException;
import jolie.lang.parse.module.exceptions.SymbolNotFoundException;
import jolie.util.Pair;

import java.net.URI;
import java.util.*;

public class SymbolReferenceResolver {
	private static CodeCheckingError buildSymbolNotFoundError( OLSyntaxNode node, String name ) {
		return CodeCheckingError.build( node, "Symbol not found: " + name
			+ " is not defined in this module (the symbol could not be retrieved from the symbol table)" );
	}

	// private static CodeCheckingError buildSymbolNotFoundError( OLSyntaxNode node, String name,
	// ImportPath path ) {
	// return CodeCheckingError.build( node, "Symbol not found: " + name + " is not defined in module "
	// + path
	// + " (the symbol could not be retrieved from the symbol table)" );
	// }

	private static CodeCheckingError buildSymbolTypeMismatchError( OLSyntaxNode node, String symbolName,
		String expectedType, String actualType ) {
		return CodeCheckingError.build( node, "Symbol is used incorrectly: " + symbolName + " is used as "
			+ expectedType + ", but it actually has type " + actualType );
	}

	private class SymbolReferenceResolverVisitor implements OLVisitor {
		private URI currentURI;
		private final List< CodeCheckingError > errors = new ArrayList<>();

		protected SymbolReferenceResolverVisitor() {}

		private void error( CodeCheckingError e ) {
			errors.add( e );
		}

		private boolean isValid() {
			return errors.isEmpty();
		}

		/**
		 * Walk through the Jolie AST tree and resolve all symbol references
		 */
		public void resolve( Program p ) throws ModuleException {
			currentURI = p.context().source();
			visit( p );
			if( !isValid() ) {
				throw new ModuleException( errors );
			}
		}

		@Override
		public void visit( Program n ) {
			for( OLSyntaxNode node : n.children() ) {
				if( !isValid() ) {
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
		public void visit( CorrelationSetInfo n ) {
			for( CorrelationVariableInfo cSetVar : n.variables() ) {
				for( CorrelationAliasInfo aliases : cSetVar.aliases() ) {
					aliases.guardName().accept( this );
				}
			}
		}

		@Override
		public void visit( InputPortInfo n ) {
			// resolve interface definition
			for( InterfaceDefinition iface : n.getInterfaceList() ) {
				Optional< SymbolInfo > symbol = getSymbol( iface.context(), iface.name() );
				if( !symbol.isPresent() ) {
					error( buildSymbolNotFoundError( iface, iface.name() ) );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					error( buildSymbolTypeMismatchError( iface, iface.name(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() ) );
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
					error( buildSymbolNotFoundError( iface, iface.name() ) );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					error( buildSymbolTypeMismatchError( iface, iface.name(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() ) );
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
					error( buildSymbolNotFoundError( n, n.id() ) );
					return;
				}
				if( !(targetSymbolInfo.get().node() instanceof TypeDefinition) ) {
					error( buildSymbolTypeMismatchError( n, n.id(), "TypeDefinition",
						targetSymbolInfo.get().node().getClass().getSimpleName() ) );
					return;
				}
				linkedType = (TypeDefinition) targetSymbolInfo.get().node();
				if( linkedType.equals( n ) ) {
					error( buildSymbolNotFoundError( n, n.id() ) );
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
					error( buildSymbolNotFoundError( iface, iface.name() ) );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					error( buildSymbolTypeMismatchError( iface, iface.name(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() ) );
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
					error( buildSymbolNotFoundError( iface, iface.name() ) );
					return;
				}
				if( !(symbol.get().node() instanceof InterfaceDefinition) ) {
					error( buildSymbolTypeMismatchError( iface, iface.name(), "InterfaceDefinition",
						symbol.get().node().getClass().getSimpleName() ) );
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
						.accept( this );
				} else if( operation instanceof InlineTreeExpressionNode.DeepCopyOperation ) {
					((InlineTreeExpressionNode.DeepCopyOperation) operation).expression()
						.accept( this );
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
			if( symbolTables.containsKey( currentURI )
				&& symbolTables.get( currentURI ).getSymbol( name ).isPresent() ) {
				return symbolTables.get( currentURI ).getSymbol( name );
			} else if( symbolTables.containsKey( context.source() )
				&& symbolTables.get( context.source() ).getSymbol( name ).isPresent() ) {
				return symbolTables.get( context.source() ).getSymbol( name );
			}
			return Optional.empty();
		}

		@Override
		public void visit( ServiceNode n ) {
			n.parameterType().ifPresent( ( type ) -> type.accept( this ) );
			n.program().accept( this );
		}
	}

	private final Map< URI, ModuleRecord > moduleMap;
	private final Map< URI, SymbolTable > symbolTables;

	private SymbolReferenceResolver( CrawlerResult moduleMap ) {
		this.moduleMap = moduleMap.toMap();
		this.symbolTables = new HashMap<>();
		for( ModuleRecord mr : this.moduleMap.values() ) {
			this.symbolTables.put( mr.uri(), mr.symbolTable() );
		}
	}

	/**
	 * perform a lookup in the module record collection for the original symbol of an importing symbol
	 * 
	 * @param symbolInfo an importing symbol
	 * @param lookedSources a set of sources that are already considered
	 * 
	 * @throws SymbolNotFoundException
	 */
	private SymbolInfo symbolSourceLookup( ImportedSymbolInfo symbolInfo, Set< URI > lookedSources )
		throws SymbolNotFoundException {
		ModuleRecord externalSourceRecord =
			this.moduleMap.get( symbolInfo.moduleSource().get().uri() );
		Optional< SymbolInfo > externalSourceSymbol =
			externalSourceRecord.symbolTable().getSymbol( symbolInfo.originalSymbolName() );
		if( !externalSourceSymbol.isPresent() || lookedSources.contains( externalSourceRecord.uri() ) ) {
			throw new SymbolNotFoundException( symbolInfo.name(), symbolInfo.importPath() );
		}
		lookedSources.add( externalSourceRecord.uri() );
		if( externalSourceSymbol.get().scope() == Scope.LOCAL ) {
			return externalSourceSymbol.get();
		} else {
			return symbolSourceLookup( (ImportedSymbolInfo) externalSourceSymbol.get(), lookedSources );
		}
	}

	private SymbolInfo symbolSourceLookup( ImportedSymbolInfo symbolInfo )
		throws SymbolNotFoundException {
		return symbolSourceLookup( symbolInfo, new HashSet<>() );
	}

	/**
	 * resolve externalSymbol by find and set its corresponding AST node by perform lookup at
	 * ModuleRecord Map, a result from ModuleCrawler.
	 * 
	 * @throws DuplicateSymbolException
	 * @throws IllegalAccessSymbolException
	 * @throws SymbolNotFoundException
	 * 
	 */
	private void resolveExternalSymbols()
		throws SymbolNotFoundException, IllegalAccessSymbolException, DuplicateSymbolException {
		for( ModuleRecord md : moduleMap.values() ) {
			for( ImportedSymbolInfo importedSymbol : md.symbolTable().importedSymbolInfos() ) {
				if( importedSymbol instanceof WildcardImportedSymbolInfo ) {
					ModuleRecord wildcardImportedRecord =
						this.moduleMap.get( importedSymbol.moduleSource().get().uri() );
					md.symbolTable().resolveWildcardImport( (WildcardImportedSymbolInfo) importedSymbol,
						wildcardImportedRecord.symbolTable().symbols() );
				} else {
					SymbolInfo targetSymbol = symbolSourceLookup( importedSymbol );
					if( targetSymbol.accessModifier() == AccessModifier.PRIVATE ) {
						throw new IllegalAccessSymbolException( importedSymbol.name(),
							importedSymbol.importPath() );
					}
					importedSymbol.resolve( targetSymbol.node() );
				}
			}
		}
	}

	/**
	 * resolve LinkedType of each ModuleRecord AST node in the Map.
	 * 
	 * @throws ModuleException if the linked type cannot find its referencing node
	 */
	private void resolveLinkedTypes() throws ModuleException {
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
	public static void resolve( CrawlerResult moduleMap ) throws ModuleException {
		SymbolReferenceResolver resolver = new SymbolReferenceResolver( moduleMap );
		try {
			resolver.resolveExternalSymbols();
		} catch( SymbolNotFoundException | IllegalAccessSymbolException
			| DuplicateSymbolException e ) {
			throw new ModuleException( e.getMessage() );
		}
		resolver.resolveLinkedTypes();
	}
}
