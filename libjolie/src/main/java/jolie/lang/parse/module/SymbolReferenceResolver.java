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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jolie.lang.CodeCheckMessage;
import jolie.lang.Constants;
import jolie.lang.Constants.OperandType;
import jolie.lang.parse.UnitOLVisitor;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationAliasInfo;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationVariableInfo;
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
import jolie.lang.parse.ast.ImportableSymbol.AccessModifier;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InputPortInfo.AggregationItemInfo;
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
import jolie.lang.parse.ast.PvalAssignStatement;
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
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.ServiceNodeJava;
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
import jolie.lang.parse.ast.expression.CurrentValueNode;
import jolie.lang.parse.ast.expression.FreshValueExpressionNode;
import jolie.lang.parse.ast.expression.IfExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode.Operation;
import jolie.lang.parse.ast.expression.InstanceOfExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.expression.PathsExpressionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.PvalExpressionNode;
import jolie.lang.parse.ast.expression.SolicitResponseExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.expression.ValuesExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.expression.VoidExpressionNode;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.module.ModuleCrawler.CrawlerResult;
import jolie.lang.parse.module.SymbolInfo.Scope;
import jolie.lang.parse.module.exceptions.DuplicateSymbolException;
import jolie.lang.parse.module.exceptions.IllegalAccessSymbolException;
import jolie.lang.parse.module.exceptions.SymbolNotFoundException;
import jolie.util.Pair;

public class SymbolReferenceResolver {
	private static CodeCheckMessage buildSymbolNotFoundError( OLSyntaxNode node, String name ) {
		return CodeCheckMessage.buildWithoutHelp( node, "Symbol not found: " + name
			+ " is not defined in this module (the symbol could not be retrieved from the symbol table)" );
	}

	private static CodeCheckMessage buildInfiniteTypeDefinitionLinkLoop( OLSyntaxNode node, String name ) {
		return CodeCheckMessage.buildWithoutHelp( node, "Type definition link loop detected: " + name
			+ " (this might mean that the referred type has not been defined or could not be retrieved)" );
	}

	// private static CodeCheckingError buildSymbolNotFoundError( OLSyntaxNode node,
	// String name,
	// ImportPath path ) {
	// return CodeCheckingError.build( node, "Symbol not found: " + name + " is not
	// defined in module "
	// + path
	// + " (the symbol could not be retrieved from the symbol table)" );
	// }

	private static CodeCheckMessage buildSymbolTypeMismatchError( OLSyntaxNode node, String symbolName,
		String expectedType, String actualType ) {
		return CodeCheckMessage.buildWithoutHelp( node, "Symbol is used incorrectly: " + symbolName + " is used as "
			+ expectedType + ", but it actually has type " + actualType );
	}

	private static CodeCheckMessage buildMissingServiceInputportError( ServiceNode node, String portId ) {
		return CodeCheckMessage.buildWithoutHelp( node,
			"Unable to bind operations to port " + portId + ": " + node.name()
				+ " service doesn't have an inputPort with location 'local' defined." );
	}

	// private static CodeCheckingError buildTypeLinkOutOfBoundError(
	// TypeDefinitionLink node ) {
	// return CodeCheckingError.build( node, "Unable to find linked type: " +
	// node.name()
	// + " has infinite loop to it's linked type" );
	// }

	private class SymbolReferenceResolverVisitor implements UnitOLVisitor {
		private URI currentURI;
		private final List< CodeCheckMessage > errors = new ArrayList<>();

		protected SymbolReferenceResolverVisitor() {}

		private void error( CodeCheckMessage e ) {
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
		public void visit( CurrentValueNode n ) {}

		@Override
		public void visit( PathsExpressionNode n ) {}

		@Override
		public void visit( ValuesExpressionNode n ) {}

		@Override
		public void visit( PvalExpressionNode n ) {}

		@Override
		public void visit( PvalAssignStatement n ) {}

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
				ifaceDeclFromSymbol.getDocumentation().ifPresent( iface::setDocumentation );
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
				OLSyntaxNode syntaxNode = symbol.get().node();
				if( !(syntaxNode instanceof InterfaceDefinition) ) {
					error( buildSymbolTypeMismatchError( iface, iface.name(), "InterfaceDefinition",
						syntaxNode != null ? syntaxNode.getClass().getSimpleName() : null ) );
					return;
				}
				InterfaceDefinition ifaceDeclFromSymbol = (InterfaceDefinition) syntaxNode;
				ifaceDeclFromSymbol.operationsMap().values().forEach( op -> {
					iface.addOperation( op );
					n.addOperation( op );
				} );
				ifaceDeclFromSymbol.getDocumentation().ifPresent( iface::setDocumentation );
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
		public void visit( IfExpressionNode n ) {
			n.guard().accept( this );
			n.thenExpression().accept( this );
			n.elseExpression().accept( this );
		}

		@Override
		public void visit( TypeDefinitionLink n ) {
			TypeDefinition linkedType;
			if( n.linkedTypeName().equals( TypeDefinitionUndefined.UNDEFINED_KEYWORD ) ) {
				linkedType = TypeDefinitionUndefined.getInstance();
			} else {
				Optional< SymbolInfo > targetSymbolInfo = getSymbol( n.context(), n.linkedTypeName() );
				if( !targetSymbolInfo.isPresent() ) {
					error( buildSymbolNotFoundError( n, n.linkedTypeName() ) );
					return;
				}
				if( !(targetSymbolInfo.get().node() instanceof TypeDefinition) ) {
					error( buildSymbolTypeMismatchError( n, n.name(), "TypeDefinition",
						targetSymbolInfo.get().node().getClass().getSimpleName() ) );
					return;
				}
				linkedType = (TypeDefinition) targetSymbolInfo.get().node();
				if( linkedType == null ) {
					error( buildSymbolNotFoundError( n, n.linkedTypeName() ) );
					return;
				} else if( linkedType.equals( n ) ) {
					error( buildInfiniteTypeDefinitionLinkLoop( n, n.linkedTypeName() ) );
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
				ifaceDeclFromSymbol.getDocumentation().ifPresent( iface::setDocumentation );

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
				ifaceDeclFromSymbol.getDocumentation().ifPresent( iface::setDocumentation );

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
			if( symbolTables.containsKey( context.source() )
				&& symbolTables.get( context.source() ).getSymbol( name ).isPresent() ) {
				return symbolTables.get( context.source() ).getSymbol( name );
			} else if( symbolTables.containsKey( currentURI )
				&& symbolTables.get( currentURI ).getSymbol( name ).isPresent() ) {
				return symbolTables.get( currentURI ).getSymbol( name );
			}
			return Optional.empty();
		}

		@Override
		public void visit( ServiceNode n ) {
			n.parameterConfiguration().ifPresent( config -> config.type().accept( this ) );
			n.program().accept( this );
		}

		@Override
		public void visit( EmbedServiceNode n ) {
			Optional< SymbolInfo > targetSymbolInfo = getSymbol( n.context(), n.serviceName() );
			if( !targetSymbolInfo.isPresent() ) {
				error( buildSymbolNotFoundError( n, n.serviceName() ) );
				return;
			}
			if( !(targetSymbolInfo.get().node() instanceof ServiceNode) ) {
				error( buildSymbolTypeMismatchError( n, n.serviceName(), "ServiceNode",
					targetSymbolInfo.get().node().getClass().getSimpleName() ) );
				return;
			}
			ServiceNode embeddingService = (ServiceNode) targetSymbolInfo.get().node();
			embeddingService.accept( this );
			n.setService( embeddingService );
			if( n.isNewPort() ) {
				if( n.service().type() == Constants.EmbeddedServiceType.SERVICENODE ) {
					bindServiceOperationsToOutputPort( n.service(), n.bindingPort() );
				} else if( n.service().type() == Constants.EmbeddedServiceType.SERVICENODE_JAVA ) {
					ServiceNodeJava service = (ServiceNodeJava) n.service();
					if( service.inputPortInfo() != null ) {
						bindServiceOperationsToOutputPort( service, n.bindingPort() );
					} else {
						error( buildMissingServiceInputportError( service, n.bindingPort().id() ) );
					}
				}
			}
		}

		@Override
		public void visit( SolicitResponseExpressionNode n ) {}
	}

	/**
	 * Scans through inputPorts with 'local' location in the service node n, and assign the interfaces
	 * and operation to an outputPort op
	 *
	 * @param n service to perform operations binding
	 * @param op target outputport
	 */
	private void bindServiceOperationsToOutputPort( ServiceNode n, OutputPortInfo op ) {
		// binds operation from ServiceNode to port
		InterfacesAndOperations publicIfacesAndOps = getInterfacesFromInputPortLocal( n );
		for( InterfaceDefinition iface : publicIfacesAndOps.interfaces() ) {
			op.addInterface( iface );
			iface.operationsMap().values().forEach( op::addOperation );
		}
		for( OperationDeclaration oper : publicIfacesAndOps.operations() ) {
			op.addOperation( oper );
		}
		for( AggregationItemInfo oper : publicIfacesAndOps.aggregationInfo() ) {
			if( oper.interfaceExtender() != null ) {
				op.addInterfaceExtender( oper.interfaceExtender() );
			}

			for( String opName : oper.outputPortList() ) {
				// adds operations and interface of the aggregated OutputPort to embedding port
				n.program()
					.children()
					.stream()
					.filter( ol -> ol instanceof OutputPortInfo && ((OutputPortInfo) ol).id().equals( opName ) )
					.findFirst()
					.ifPresent( o -> {
						OutputPortInfo aggregatedOp = (OutputPortInfo) o;
						aggregatedOp.getInterfaceList().forEach( op::addInterface );
						aggregatedOp.operations().forEach( op::addOperation );
					} );
			}
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
	 */
	private SymbolInfo symbolSourceLookup( ImportedSymbolInfo symbolInfo, Set< URI > lookedSources )
		throws SymbolNotFoundException {
		ModuleRecord externalSourceRecord = this.moduleMap.get( symbolInfo.moduleSource().get().uri() );
		if( externalSourceRecord == null ) {
			throw new SymbolNotFoundException( symbolInfo.originalSymbolName(),
				symbolInfo.moduleSource().get().uri().toString() );
		}
		Optional< SymbolInfo > externalSourceSymbol = externalSourceRecord.symbolTable()
			.getSymbol( symbolInfo.originalSymbolName() );
		if( !externalSourceSymbol.isPresent() || lookedSources.contains( externalSourceRecord.uri() ) ) {
			throw new SymbolNotFoundException( symbolInfo.originalSymbolName(), symbolInfo.importPath() );
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
	 */
	private void resolveExternalSymbols()
		throws SymbolNotFoundException, IllegalAccessSymbolException, DuplicateSymbolException {
		for( ModuleRecord md : moduleMap.values() ) {
			for( ImportedSymbolInfo importedSymbol : md.symbolTable().importedSymbolInfos() ) {
				if( importedSymbol instanceof WildcardImportedSymbolInfo ) {
					ModuleRecord wildcardImportedRecord =
						this.moduleMap.get( importedSymbol.moduleSource().get().uri() );

					// Resolve the target module referencing before importing in to the main module record,
					// So it do not need the extra information on the importing symbols.
					SymbolReferenceResolverVisitor resolver = new SymbolReferenceResolverVisitor();
					resolver.visit( wildcardImportedRecord.program() );

					md.symbolTable().resolveWildcardImport( (WildcardImportedSymbolInfo) importedSymbol,
						wildcardImportedRecord.symbolTable().symbols() );
				} else if( importedSymbol.node() == null ) {
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
			CodeCheckMessage message = CodeCheckMessage.withoutHelp( null, e.getMessage() );
			throw new ModuleException( message );
		}
		resolver.resolveLinkedTypes();
	}

	/**
	 * An utility class represents interfaces and operations
	 */
	private static class InterfacesAndOperations {
		private final List< InterfaceDefinition > ifaces;
		private final List< OperationDeclaration > ops;
		private final List< InputPortInfo.AggregationItemInfo > aggregationInfo;

		private InterfacesAndOperations() {
			ifaces = new ArrayList<>();
			ops = new ArrayList<>();
			aggregationInfo = new ArrayList<>();
		}

		public InterfaceDefinition[] interfaces() {
			return ifaces.toArray( new InterfaceDefinition[] {} );
		}

		public OperationDeclaration[] operations() {
			return ops.toArray( new OperationDeclaration[] {} );
		}

		public InputPortInfo.AggregationItemInfo[] aggregationInfo() {
			return aggregationInfo.toArray( new InputPortInfo.AggregationItemInfo[] {} );
		}
	}

	/**
	 * retrieves InterfaceDefinitions from ServiceNode's inputPorts declared with 'local' location
	 *
	 * @param node a service node object
	 * @return InterfaceDefinition[] list of interfaces of local incoming communication port
	 *
	 */
	private static InterfacesAndOperations getInterfacesFromInputPortLocal(
		ServiceNode node ) {
		InterfacesAndOperations result = new InterfacesAndOperations();

		for( OLSyntaxNode n : node.program().children() ) {
			if( n instanceof InputPortInfo ) {
				InputPortInfo ip = (InputPortInfo) n;
				if( ip.location() instanceof ConstantStringExpression ) {
					String location = ((ConstantStringExpression) ip.location()).value();
					if( location.equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
						result.ifaces.addAll( ip.getInterfaceList() );
						result.aggregationInfo.addAll( Arrays.asList( ip.aggregationList() ) );
						result.ops.addAll( ip.operations() );
					}
				}
			}
		}
		return result;
	}
}
