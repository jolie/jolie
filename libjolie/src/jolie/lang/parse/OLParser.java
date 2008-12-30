/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/
package jolie.lang.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jolie.Constants;
import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallFunctionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OrConditionNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.OperationCollector;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.util.Pair;

/** Parser for a .ol file. 
 * @author Fabrizio Montesi
 *
 */
public class OLParser extends AbstractParser
{
	public class Interface implements OperationCollector
	{
		final private Map<String, OperationDeclaration> operationsMap =
			new HashMap<String, OperationDeclaration>();

		public Map<String, OperationDeclaration> operationsMap()
		{
			return operationsMap;
		}

		public void addOperation( OperationDeclaration decl )
		{
			operationsMap.put( decl.id(), decl );
		}

		public void copyTo( OperationCollector oc )
		{
			oc.operationsMap().putAll( operationsMap );
		}
	}
	private final Program program = new Program( new ParsingContext() );
	private final Map<String, Scanner.Token> constantsMap =
		new HashMap<String, Scanner.Token>();
	private boolean insideInstallFunction = false;
	private String[] includePaths;
	private final Map<String, Interface> interfaces =
		new HashMap<String, Interface>();
	
	private final ClassLoader classLoader;

	public OLParser( Scanner scanner, String[] includePaths, ClassLoader classLoader )
	{
		super( scanner );
		this.includePaths = includePaths;
		this.classLoader = classLoader;
	}

	public Program parse()
		throws IOException, ParserException
	{
		getToken();
		Scanner.Token t;
		do {
			t = token;
			parseInclude();
			parseConstants();
			parseInclude();
			parseExecution();
			parseInclude();
			parseCorrelationSet();
			parseInclude();
			parseInterfaces();
			parseInclude();
			parsePorts();
			parseInclude();
			parseEmbedded();
			parseInclude();
			parseCode();
		} while ( t != token );

		if ( t.isNot( Scanner.TokenType.EOF ) ) {
			throwException( "Invalid token encountered" );
		}
		return program;
	}

	private void parseEmbedded()
		throws IOException, ParserException
	{
		if ( token.isKeyword( "embedded" ) ) {
			String servicePath, portId;
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			boolean keepRun = true;
			Constants.EmbeddedServiceType type;
			while ( keepRun ) {
				type = null;
				if ( token.isKeyword( "Java" ) ) {
					type = Constants.EmbeddedServiceType.JAVA;
				} else if ( token.isKeyword( "Jolie" ) ) {
					type = Constants.EmbeddedServiceType.JOLIE;
				} else if ( token.isKeyword( "JavaScript" ) ) {
					type = Constants.EmbeddedServiceType.JAVASCRIPT;
				}
				if ( type == null ) {
					keepRun = false;
				} else {
					getToken();
					eat( Scanner.TokenType.COLON, "expected : after embedded service type" );
					checkConstant();
					while ( token.is( Scanner.TokenType.STRING ) ) {
						servicePath = token.content();
						getToken();
						if ( token.isKeyword( "in" ) ) {
							eatKeyword( "in", "expected in" );
							assertToken( Scanner.TokenType.ID, "expected output port name" );
							portId = token.content();
							getToken();
						} else {
							portId = null;
						}
						program.addChild(
							new EmbeddedServiceNode(
							getContext(),
							type,
							servicePath,
							portId ) );
						if ( token.is( Scanner.TokenType.COMMA ) ) {
							getToken();
						} else {
							break;
						}
					}
				}
			}
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}

	private void parseCorrelationSet()
		throws IOException, ParserException
	{
		if ( token.isKeyword( "cset" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			String varId;
			Set<List<VariablePathNode>> cset = new HashSet<List<VariablePathNode>>();
			List<VariablePathNode> list;
			while ( token.is( Scanner.TokenType.ID ) ) {
				list = new Vector<VariablePathNode>();
				varId = token.content();
				getToken();
				list.add( parseVariablePath( varId ) );
				if ( token.is( Scanner.TokenType.COLON ) ) {
					getToken();
					while ( token.is( Scanner.TokenType.ID ) ) {
						varId = token.content();
						getToken();
						list.add( parseVariablePath( varId ) );
					}
				}
				cset.add( list );
				if ( token.is( Scanner.TokenType.COMMA ) ) {
					getToken();
				} else {
					break;
				}
			}

			program.addChild( new CorrelationSetInfo( getContext(), cset ) );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}

	private void parseExecution()
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.EXECUTION ) ) {
			Constants.ExecutionMode mode = Constants.ExecutionMode.SEQUENTIAL;
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			if ( token.is( Scanner.TokenType.SEQUENTIAL ) ) {
				mode = Constants.ExecutionMode.SEQUENTIAL;
			} else if ( token.is( Scanner.TokenType.CONCURRENT ) ) {
				mode = Constants.ExecutionMode.CONCURRENT;
			} else if ( token.is( Scanner.TokenType.SINGLE ) ) {
				mode = Constants.ExecutionMode.SINGLE;
			} else {
				throwException( "Expected execution mode, found " + token.content() );
			}
			program.addChild( new ExecutionInfo( getContext(), mode ) );
			getToken();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}

	private void parseConstants()
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.CONSTANTS ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			boolean keepRun = true;
			while ( token.is( Scanner.TokenType.ID ) && keepRun ) {
				String cId = token.content();
				getToken();
				eat( Scanner.TokenType.ASSIGN, "expected =" );
				if ( token.isNot( Scanner.TokenType.STRING ) &&
					token.isNot( Scanner.TokenType.INT ) &&
					token.isNot( Scanner.TokenType.ID ) ) {
					throwException( "expected string, integer or identifier constant" );
				}
				constantsMap.put( cId, token );
				getToken();
				if ( token.isNot( Scanner.TokenType.COMMA ) ) {
					keepRun = false;
				} else {
					getToken();
				}
			}
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}

	private void parseInclude()
		throws IOException, ParserException
	{
		String[] origIncludePaths;
		while ( token.is( Scanner.TokenType.INCLUDE ) ) {
			getToken();
			Scanner oldScanner = scanner();
			assertToken( Scanner.TokenType.STRING, "expected filename to include" );
			String includeStr = token.content();
			InputStream stream = null;
			File f = null;
			for ( int i = 0; i < includePaths.length && stream == null; i++ ) {
				f = new File(
					includePaths[i] +
					jolie.Constants.fileSeparator +
					includeStr );
				if ( f.exists() ) {
					stream = new FileInputStream( f );
				}
			}

			if ( stream == null ) {
				URL includeURL = classLoader.getResource( includeStr );
				if ( includeURL == null ) {
					throwException( "File not found: " + includeStr );
				} else {
					stream = includeURL.openStream();
				}
			}
			origIncludePaths = includePaths;
			setScanner( new Scanner( stream, includeStr ) );
			
			includePaths = Arrays.copyOf( origIncludePaths, origIncludePaths.length + 1 );
			includePaths[ origIncludePaths.length ] = f.getParent();
			parse();
			includePaths = origIncludePaths;
			setScanner( oldScanner );
			getToken();
		}
	}

	private boolean checkConstant()
	{
		if ( token.is( Scanner.TokenType.ID ) ) {
			Scanner.Token t = null;
			Constants.Predefined p = Constants.Predefined.get( token.content() );
			if ( p != null ) {
				t = p.token();
			} else {
				t = constantsMap.get( token.content() );
			}
			if ( t != null ) {
				token = t;
				return true;
			}
		}
		return false;
	}

	private void parsePorts()
		throws IOException, ParserException
	{
		boolean keepRun = true;
		while ( keepRun ) {
			if ( token.isKeyword( "inputPort" ) ) {
				parseInputPortInfo();
			} else if ( token.isKeyword( "outputPort" ) ) {
				getToken();
				assertToken( Scanner.TokenType.ID, "expected output port identifier" );
				OutputPortInfo p = new OutputPortInfo( getContext(), token.content() );
				getToken();
				eat( Scanner.TokenType.LCURLY, "expected {" );
				parseOutputPortInfo( p );
				program.addChild( p );
				eat( Scanner.TokenType.RCURLY, "expected }" );
			} else {
				keepRun = false;
			}
		}
	}

	private void parseInputPortInfo()
		throws IOException, ParserException
	{
		String inputPortName;
		String protocolId;
		URI inputPortLocation;
		OLSyntaxNode protocolConfiguration;

		getToken();
		assertToken( Scanner.TokenType.ID, "expected inputPort name" );
		inputPortName = token.content();
		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		Interface iface = new Interface();
		inputPortLocation = null;
		protocolId = null;
		protocolConfiguration = null;
		Map<String, String> redirectionMap = new HashMap<String, String>();
		while ( token.isNot( Scanner.TokenType.RCURLY ) ) {
			if ( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( iface );
			} else if ( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( iface );
			} else if ( token.isKeyword( "Location" ) ) {
				if ( inputPortLocation != null ) {
					throwException( "Location already defined for service " + inputPortName );
				}
				getToken();
				eat( Scanner.TokenType.COLON, "expected : after Location" );
				checkConstant();
				assertToken( Scanner.TokenType.STRING, "expected inputPort location string" );
				try {
					inputPortLocation = new URI( token.content() );
				} catch ( URISyntaxException e ) {
					throwException( e );
				}
				getToken();
			} else if ( token.isKeyword( "Interfaces" ) ) {
				getToken();
				eat( Scanner.TokenType.COLON, "expected : after Interfaces" );
				boolean keepRun = true;
				while( keepRun ) {
					assertToken( Scanner.TokenType.ID, "expected interface name" );
					Interface i = interfaces.get( token.content() );
					if ( i == null ) {
						throwException( "Invalid interface name: " + token.content() );
					}
					i.copyTo( iface );
					getToken();
					
					if ( token.is( Scanner.TokenType.COMMA ) ) {
						getToken();
					} else {
						keepRun = false;
					}
				}
			} else if ( token.isKeyword( "Protocol" ) ) {
				if ( protocolId != null ) {
					throwException( "Protocol already defined for inputPort " + inputPortName );
				}
				getToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				checkConstant();
				assertToken( Scanner.TokenType.ID, "expected protocol identifier" );
				protocolId = token.content();
				getToken();
				if ( token.is( Scanner.TokenType.LCURLY ) ) {
					addTokens( Arrays.asList(
						new Scanner.Token( Scanner.TokenType.ID, Constants.GLOBAL ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, Constants.INPUT_PORTS_NODE_NAME ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, inputPortName ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, Constants.PROTOCOL_NODE_NAME ),
						token ) );
					// Protocol configuration
					getToken();
					protocolConfiguration = parseInVariablePathProcess( false );
				}
			} else if ( token.isKeyword( "Redirects" ) ) {
				getToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				String subLocationName;
				while ( token.is( Scanner.TokenType.ID ) ) {
					subLocationName = token.content();
					getToken();
					eat( Scanner.TokenType.ARROW, "expected =>" );
					assertToken( Scanner.TokenType.ID, "expected outputPort identifier" );
					redirectionMap.put( subLocationName, token.content() );
					getToken();
					if ( token.is( Scanner.TokenType.COMMA ) ) {
						getToken();
					} else {
						break;
					}
				}
			} else {
				throwException( "Unrecognized token in inputPort " + inputPortName );
			}
		}
		eat( Scanner.TokenType.RCURLY, "} expected" );
		if ( inputPortLocation == null ) {
			throwException( "expected location URI for " + inputPortName );
		} else if ( iface.operationsMap.isEmpty() && redirectionMap.isEmpty() ) {
			throwException( "expected at least one operation, interface or redirection for service " + inputPortName );
		} else if ( protocolId == null && !inputPortLocation.toString().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			throwException( "expected protocol for inputPort " + inputPortName );
		}
		InputPortInfo iport = new InputPortInfo( getContext(), inputPortName, inputPortLocation, protocolId, protocolConfiguration, redirectionMap );
		iface.copyTo( iport );
		program.addChild( iport );
	}

	private void parseInterfaces()
		throws IOException, ParserException
	{
		String name;
		Interface iface;
		while ( token.isKeyword( "interface" ) ) {
			getToken();
			assertToken( Scanner.TokenType.ID, "expected interface name" );
			name = token.content();
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			iface = new Interface();
			parseOperations( iface );
			interfaces.put( name, iface );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}

	private void parseOperations( OperationCollector oc )
		throws IOException, ParserException
	{
		boolean keepRun = true;
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( oc );
			} else if ( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( oc );
			} else {
				keepRun = false;
			}
		}
	}

	private void parseOutputPortInfo( OutputPortInfo p )
		throws IOException, ParserException
	{
		boolean keepRun = true;
		while ( keepRun ) {
			if ( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( p );
			} else if ( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( p );
			} else if ( token.isKeyword( "Interfaces" ) ) {
				getToken();
				eat( Scanner.TokenType.COLON, "expected : after Interfaces" );
				boolean r = true;
				while( r ) {
					assertToken( Scanner.TokenType.ID, "expected interface name" );
					Interface i = interfaces.get( token.content() );
					if ( i == null ) {
						throwException( "Invalid interface name: " + token.content() );
					}
					i.copyTo( p );
					getToken();
					
					if ( token.is( Scanner.TokenType.COMMA ) ) {
						getToken();
					} else {
						r = false;
					}
				}
			} else if ( token.isKeyword( "Location" ) ) {
				if ( p.location() != null ) {
					throwException( "Location already defined for output port " + p.id() );
				}

				getToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				checkConstant();

				assertToken(
					Scanner.TokenType.STRING, "expected location string" );
				URI location = null;
				try {
					location = new URI( token.content() );
				} catch ( URISyntaxException e ) {
					throwException( e );
				}

				p.setLocation( location );
				getToken();

			} else if ( token.isKeyword( "Protocol" ) ) {
				if ( p.protocolId() != null ) {
					throwException( "Protocol already defined for output port " + p.id() );
				}

				getToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				checkConstant();

				assertToken( Scanner.TokenType.ID, "expected protocol identifier" );
				p.setProtocolId( token.content() );
				getToken();

				if ( token.is( Scanner.TokenType.LCURLY ) ) {
					addTokens( Arrays.asList(
						new Scanner.Token( Scanner.TokenType.ID, p.id() ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, "protocol" ),
						token ) );
					// Protocol configuration
					getToken();

					p.setProtocolConfiguration( parseInVariablePathProcess( false ) );
				}

			} else {
				keepRun = false;
			}

		}
	}

	private void parseOneWayOperations( OperationCollector oc )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );

		boolean keepRun = true;

		while ( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				oc.addOperation( new OneWayOperationDeclaration( getContext(), token.content() ) );
				getToken();

				if ( token.is( Scanner.TokenType.COMMA ) ) {
					getToken();
				} else {
					keepRun = false;
				}

			} else {
				keepRun = false;
			}

		}
	}

	private void parseRequestResponseOperations( OperationCollector oc )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );

		boolean keepRun = true;
		String opId;

		while ( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				getToken();

				Vector<String> faultNames = new Vector<String>();
				if ( token.is( Scanner.TokenType.THROWS ) ) {
					getToken();
					while ( token.is( Scanner.TokenType.ID ) ) {
						faultNames.add( token.content() );
						getToken();

					}
				}
				oc.addOperation( new RequestResponseOperationDeclaration( getContext(), opId, faultNames ) );
				if ( token.is( Scanner.TokenType.COMMA ) ) {
					getToken();
				} else {
					keepRun = false;
				}
			} else {
				keepRun = false;
			}

		}
	}
	
	private SequenceStatement initSequence = null;

	private void parseCode()
		throws IOException, ParserException
	{
		boolean mainDefined = false;
		boolean keepRun = true;

		do {
			if ( token.is( Scanner.TokenType.DEFINE ) ) {
				program.addChild( parseDefinition() );
			} else if ( token.isKeyword( "main" ) ) {
				if ( mainDefined ) {
					throwException( "you must specify only one main definition" );
				}

				program.addChild( parseMain() );
				mainDefined = true;
			} else if ( token.is( Scanner.TokenType.INIT ) ) {
				if ( initSequence == null ) {
					initSequence = new SequenceStatement( getContext() );
					program.addChild( new DefinitionNode( getContext(), "init", initSequence ) );
				}

				initSequence.addChild( parseInit() );
			} else {
				keepRun = false;
			}

		} while ( keepRun );
	}

	private DefinitionNode parseMain()
		throws IOException, ParserException
	{
		getToken();
		eat(
			Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		DefinitionNode retVal = new DefinitionNode( getContext(), "main", parseProcess() );
		eat(
			Scanner.TokenType.RCURLY, "expected } after procedure definition" );
		return retVal;
	}

	private OLSyntaxNode parseInit()
		throws IOException, ParserException
	{
		getToken();
		eat(
			Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		OLSyntaxNode retVal = parseProcess();
		eat(
			Scanner.TokenType.RCURLY, "expected } after procedure definition" );
		return retVal;
	}

	private DefinitionNode parseDefinition()
		throws IOException, ParserException
	{
		getToken();
		assertToken(
			Scanner.TokenType.ID, "expected definition identifier" );
		String definitionId = token.content();
		getToken();

		eat(
			Scanner.TokenType.LCURLY, "expected { after definition declaration" );
		DefinitionNode retVal =
			new DefinitionNode(
			getContext(),
			definitionId,
			parseProcess() );
		eat(
			Scanner.TokenType.RCURLY, "expected } after definition declaration" );

		return retVal;
	}

	public OLSyntaxNode parseProcess()
		throws IOException, ParserException
	{
		return parseParallelStatement();
	}

	private ParallelStatement parseParallelStatement()
		throws IOException, ParserException
	{
		ParallelStatement stm = new ParallelStatement( getContext() );
		stm.addChild( parseSequenceStatement() );
		while ( token.is( Scanner.TokenType.PARALLEL ) ) {
			getToken();
			stm.addChild( parseSequenceStatement() );
		}

		return stm;
	}

	private SequenceStatement parseSequenceStatement()
		throws IOException, ParserException
	{
		SequenceStatement stm = new SequenceStatement( getContext() );

		stm.addChild( parseBasicStatement() );
		while ( token.is( Scanner.TokenType.SEQUENCE ) ) {
			getToken();
			stm.addChild( parseBasicStatement() );
		}

		return stm;
	}
	private Vector<Vector<Scanner.Token>> inVariablePaths = new Vector<Vector<Scanner.Token>>();

	private OLSyntaxNode parseInVariablePathProcess(
		boolean withConstruct )
		throws IOException, ParserException
	{
		OLSyntaxNode ret = null;
		Vector<Scanner.Token> tokens = new Vector<Scanner.Token>();

		if ( withConstruct ) {
			eat( Scanner.TokenType.LPAREN, "expected (" );

			while ( token.isNot( Scanner.TokenType.LCURLY ) ) {
				tokens.add( token );
				getToken();

			}
			//TODO transfer this whole buggy thing to the OOIT

			tokens.remove( tokens.size() - 1 );
		//getToken();
		} else {
			while ( token.isNot( Scanner.TokenType.LCURLY ) ) {
				tokens.add( token );
				getToken();

			}








		}
		inVariablePaths.add( tokens );

		eat(
			Scanner.TokenType.LCURLY, "expected {" );
		ret =
			parseProcess();
		eat(
			Scanner.TokenType.RCURLY, "expected }" );
		inVariablePaths.remove( inVariablePaths.size() - 1 );

		return ret;
	}

	private OLSyntaxNode parseBasicStatement()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;

		if ( token.is( Scanner.TokenType.LSQUARE ) ) {
			retVal = parseNDChoiceStatement();
		} else if ( token.is( Scanner.TokenType.ID ) ) {
			checkConstant();
			String id = token.content();
			getToken();

			if ( token.is( Scanner.TokenType.COLON ) || token.is( Scanner.TokenType.LSQUARE ) || token.is( Scanner.TokenType.DOT ) || token.is( Scanner.TokenType.ASSIGN ) || token.is( Scanner.TokenType.POINTS_TO ) || token.is( Scanner.TokenType.DEEP_COPY_LEFT ) || token.is( Scanner.TokenType.DECREMENT ) || token.is( Scanner.TokenType.CHOICE ) ) {
				retVal = parseAssignOrDeepCopyOrPointerStatement( parseVariablePath( id ) );
			} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
				retVal = parseInputOperationStatement( id );
			} else if ( token.is( Scanner.TokenType.AT ) ) {
				getToken();
				retVal =
					parseOutputOperationStatement( id );
			} else {
				retVal = new DefinitionCallStatement( getContext(), id );
			}
		} else if ( token.is( Scanner.TokenType.WITH ) ) {
			getToken();
			retVal =
				parseInVariablePathProcess( true );
		} else if ( token.is( Scanner.TokenType.DOT ) && inVariablePaths.size() > 0 ) {
			retVal = parseAssignOrDeepCopyOrPointerStatement( parsePrefixedVariablePath() );
		} else if ( token.is( Scanner.TokenType.CHOICE ) ) { // Pre increment: ++i
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new PreIncrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) { // Pre decrement
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new PreDecrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.SYNCHRONIZED ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected lock id" );
			String id = token.content();
			getToken();

			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			eat(
				Scanner.TokenType.LCURLY, "expected {" );
			retVal =
				new SynchronizedStatement( getContext(), id, parseProcess() );
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
		} else if ( token.is( Scanner.TokenType.UNDEF ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String id = token.content();
			getToken();

			retVal =
				new UndefStatement( getContext(), parseVariablePath( id ) );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.FOR ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			OLSyntaxNode init = parseProcess();
			eat(
				Scanner.TokenType.COMMA, "expected ," );
			OLSyntaxNode condition = parseCondition();
			eat(
				Scanner.TokenType.COMMA, "expected ," );
			OLSyntaxNode post = parseProcess();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );

			OLSyntaxNode body = parseBasicStatement();

			retVal =
				new ForStatement( getContext(), init, condition, post, body );
		} else if ( token.is( Scanner.TokenType.SPAWN ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String id = token.content();
			getToken();
			VariablePathNode indexVariablePath = parseVariablePath( id );
			assertToken( Scanner.TokenType.ID, "expected over" );
			if ( token.isKeyword( "over" ) == false ) {
				throwException( "expected over" );
			}
			getToken();
			OLSyntaxNode upperBoundExpression = parseExpression();
			eat( Scanner.TokenType.RPAREN, "expected )" );

			VariablePathNode inVariablePath = null;
			if ( token.isKeyword( "in" ) ) {
				getToken();
				id = token.content();
				getToken();
				inVariablePath = parseVariablePath( id );
			}
			eat( Scanner.TokenType.LCURLY, "expected {" );
			OLSyntaxNode process = parseProcess();
			eat( Scanner.TokenType.RCURLY, "expected }" );
			retVal = new SpawnStatement(
				getContext(),
				indexVariablePath,
				upperBoundExpression,
				inVariablePath,
				process
			);
		} else if ( token.is( Scanner.TokenType.FOREACH ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected variable path" );
			String varId = token.content();
			getToken();

			VariablePathNode keyPath = parseVariablePath( varId );
			eat(
				Scanner.TokenType.COLON, "expected :" );
			assertToken(
				Scanner.TokenType.ID, "expected variable path" );
			varId =
				token.content();
			getToken();

			VariablePathNode targetPath = parseVariablePath( varId );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );

			OLSyntaxNode body = parseBasicStatement();

			retVal =
				new ForEachStatement( getContext(), keyPath, targetPath, body );
		} else if ( token.is( Scanner.TokenType.RUN ) ) { // run( <Expression> )
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			retVal =
				new RunStatement( getContext(), parseExpression() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.LINKIN ) ) {
			retVal = parseLinkInStatement();
		} else if ( token.is( Scanner.TokenType.CURRENT_HANDLER ) ) {
			getToken();
			retVal =
				new CurrentHandlerStatement( getContext() );
		} else if ( token.is( Scanner.TokenType.NULL_PROCESS ) ) {
			getToken();
			retVal =
				new NullProcessStatement( getContext() );
		} else if ( token.is( Scanner.TokenType.EXIT ) ) {
			getToken();
			retVal =
				new ExitStatement( getContext() );
		} else if ( token.is( Scanner.TokenType.WHILE ) ) {
			retVal = parseWhileStatement();
		} else if ( token.is( Scanner.TokenType.LINKOUT ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected link identifier" );
			retVal =
				new LinkOutStatement( getContext(), token.content() );
			getToken();

			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal =
				parseProcess();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.LCURLY ) ) {
			getToken();
			retVal =
				parseProcess();
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
		} else if ( token.is( Scanner.TokenType.SCOPE ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected scope identifier" );
			String id = token.content();
			getToken();

			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			eat(
				Scanner.TokenType.LCURLY, "expected {" );
			retVal =
				new Scope( getContext(), id, parseProcess() );
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
		} else if ( token.is( Scanner.TokenType.COMPENSATE ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected scope identifier" );
			retVal =
				new CompensateStatement( getContext(), token.content() );
			getToken();

			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.THROW ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected fault identifier" );
			String faultName = token.content();
			getToken();

			if ( token.is( Scanner.TokenType.RPAREN ) ) {
				retVal = new ThrowStatement( getContext(), faultName );
			} else {
				eat( Scanner.TokenType.COMMA, "expected , or )" );
				OLSyntaxNode expression = parseExpression();
				/*assertToken( Scanner.TokenType.ID, "expected variable path" );
				String varId = token.content();
				getToken();
				VariablePathNode path = parseVariablePath( varId );*/
				retVal =
					new ThrowStatement( getContext(), faultName, expression );
			}

			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.INSTALL ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			retVal =
				new InstallStatement( getContext(), parseInstallFunction() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IF ) ) {
			IfStatement stm = new IfStatement( getContext() );
			OLSyntaxNode cond;

			OLSyntaxNode node;

			getToken();

			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			cond =
				parseCondition();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			node =
				parseBasicStatement();
			stm.addChild(
				new Pair<OLSyntaxNode, OLSyntaxNode>( cond, node ) );

			boolean keepRun = true;
			while ( token.is( Scanner.TokenType.ELSE ) && keepRun ) {
				getToken();
				if ( token.is( Scanner.TokenType.IF ) ) { // else if branch
					getToken();
					eat(
						Scanner.TokenType.LPAREN, "expected (" );
					cond =
						parseCondition();
					eat(
						Scanner.TokenType.RPAREN, "expected )" );
					node =
						parseBasicStatement();
					stm.addChild(
						new Pair<OLSyntaxNode, OLSyntaxNode>( cond, node ) );
				} else { // else branch
					keepRun = false;
					stm.setElseProcess( parseBasicStatement() );
				}

			}

			retVal = stm;
		}

		if ( retVal == null ) {
			throwException( "expected basic statement" );
		}

		return retVal;
	}

	private InstallFunctionNode parseInstallFunction()
		throws IOException, ParserException
	{
		boolean backup = insideInstallFunction;
		insideInstallFunction = true;
		Vector< Pair< String, OLSyntaxNode > > vec =
			new Vector< Pair< String, OLSyntaxNode > >();

		boolean keepRun = true;
		Vector< String > names = new Vector< String >();
		OLSyntaxNode handler;
		while( keepRun ) {
			do {
				if ( token.is( Scanner.TokenType.THIS ) ) {
					names.add( null );
				} else if ( token.is( Scanner.TokenType.ID ) ) {
					names.add( token.content() );
				} else {
					throwException( "expected fault identifier or this" );
				}
				getToken();
			} while( token.isNot( Scanner.TokenType.ARROW ) );
			getToken(); // eat the arrow
			handler = parseProcess();
			for( String name : names ) {
				vec.add( new Pair< String, OLSyntaxNode >( name, handler ) );
			}
			names.clear();
			
			if ( token.is( Scanner.TokenType.COMMA ) ) {
				getToken();
			} else {
				keepRun = false;
			}
		}

		insideInstallFunction = backup;
		return new InstallFunctionNode( vec.toArray( new Pair[]{} ) );
	}

	private OLSyntaxNode parseAssignOrDeepCopyOrPointerStatement( VariablePathNode path )
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;

		if ( token.is( Scanner.TokenType.ASSIGN ) ) {
			getToken();
			retVal =
				new AssignStatement( getContext(), path, parseExpression() );
		} else if ( token.is( Scanner.TokenType.CHOICE ) ) {
			getToken();
			retVal =
				new PostIncrementStatement( getContext(), path );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) {
			getToken();
			retVal =
				new PostDecrementStatement( getContext(), path );
		} else if ( token.is( Scanner.TokenType.POINTS_TO ) ) {
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String id = token.content();
			getToken();

			retVal =
				new PointerStatement( getContext(), path, parseVariablePath( id ) );
		} else if ( token.is( Scanner.TokenType.DEEP_COPY_LEFT ) ) {
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String id = token.content();
			getToken();

			retVal =
				new DeepCopyStatement( getContext(), path, parseVariablePath( id ) );
		} else {
			throwException( "expected = or -> or << or -- or ++" );
		}

		return retVal;
	}

	private VariablePathNode parseVariablePath( String varId )
		throws IOException, ParserException
	{
		OLSyntaxNode expr = null;
		VariablePathNode path = null;

		if ( varId.equals( Constants.GLOBAL ) ) {
			path = new VariablePathNode( getContext(), true );
		} else {
			path = new VariablePathNode( getContext(), false );
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				getToken();
				expr =
					parseExpression();
				eat(
					Scanner.TokenType.RSQUARE, "expected ]" );
			}

			path.append( new Pair<OLSyntaxNode, OLSyntaxNode>(
				new ConstantStringExpression( getContext(), varId ), expr ) );
		}

		OLSyntaxNode nodeExpr = null;
		while ( token.is( Scanner.TokenType.DOT ) ) {
			getToken();
			if ( token.is( Scanner.TokenType.ID ) ) {
				nodeExpr = new ConstantStringExpression( getContext(), token.content() );
			} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
				getToken();
				nodeExpr =
					parseExpression();
				assertToken(
					Scanner.TokenType.RPAREN, "expected )" );
			} else {
				assertToken( Scanner.TokenType.ID, "expected nested node identifier" );
			}

			getToken();
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				getToken();
				expr =
					parseExpression();
				eat(
					Scanner.TokenType.RSQUARE, "expected ]" );
			} else {
				expr = null;
			}

			path.append(
				new Pair<OLSyntaxNode, OLSyntaxNode>( nodeExpr, expr ) );
		}

		return path;
	}

	private VariablePathNode parsePrefixedVariablePath()
		throws IOException, ParserException
	{
		int i = inVariablePaths.size() - 1;
		Vector<Scanner.Token> tokens = new Vector<Scanner.Token>();
		tokens.addAll( inVariablePaths.get( i ) );

		while ( tokens.firstElement().is( Scanner.TokenType.DOT ) ) {
			i--;
			tokens.addAll( 0, inVariablePaths.get( i ) );
		}

		addTokens( tokens );
		addTokens(
			Arrays.asList( new Scanner.Token( Scanner.TokenType.DOT ) ) );
		getToken();

		String varId = token.content();
		getToken();

		return parseVariablePath( varId );
	}

	private NDChoiceStatement parseNDChoiceStatement()
		throws IOException, ParserException
	{
		NDChoiceStatement stm = new NDChoiceStatement( getContext() );
		OLSyntaxNode inputGuard = null;
		OLSyntaxNode process;

		while ( token.is( Scanner.TokenType.LSQUARE ) ) {
			getToken(); // Eat [
			if ( token.is( Scanner.TokenType.LINKIN ) ) {
				inputGuard = parseLinkInStatement();
			} else if ( token.is( Scanner.TokenType.ID ) ) {
				String id = token.content();
				getToken();

				inputGuard =
					parseInputOperationStatement( id );
			} else {
				throwException( "expected input guard" );
			}

			eat( Scanner.TokenType.RSQUARE, "] expected" );
			eat(
				Scanner.TokenType.LCURLY, "expected {" );
			process =
				parseProcess();
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
			stm.addChild( new Pair<OLSyntaxNode, OLSyntaxNode>( inputGuard, process ) );
		}

		return stm;
	}

	private LinkInStatement parseLinkInStatement()
		throws IOException, ParserException
	{
		getToken();
		eat(
			Scanner.TokenType.LPAREN, "expected (" );
		assertToken(
			Scanner.TokenType.ID, "expected link identifier" );
		LinkInStatement stm = new LinkInStatement( getContext(), token.content() );
		getToken();

		eat(
			Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}

	private OLSyntaxNode parseInputOperationStatement( String id )
		throws IOException, ParserException
	{
		ParsingContext context = getContext();
		VariablePathNode inputVarPath = parseOperationVariablePathParameter();
		OLSyntaxNode stm;

		if ( token.is( Scanner.TokenType.LPAREN ) ) { // Request Response operation
			OLSyntaxNode outputExpression = parseOperationExpressionParameter();
			OLSyntaxNode process;

			eat(
				Scanner.TokenType.LCURLY, "expected {" );
			process =
				parseProcess();
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
			stm =
				new RequestResponseOperationStatement(
				context, id, inputVarPath, outputExpression, process );
		} else { // One Way operation
			stm = new OneWayOperationStatement( context, id, inputVarPath );
		}

		return stm;
	}

	/**
	 * @return The VariablePath parameter of the statement. May be null.
	 * @throws IOException
	 * @throws ParserException
	 */
	private VariablePathNode parseOperationVariablePathParameter()
		throws IOException, ParserException
	{
		VariablePathNode ret = null;

		eat(
			Scanner.TokenType.LPAREN, "expected (" );
		if ( token.is( Scanner.TokenType.ID ) ) {
			String varId = token.content();
			getToken();

			ret =
				parseVariablePath( varId );
		} else if ( token.is( Scanner.TokenType.DOT ) ) {
			ret = parsePrefixedVariablePath();
		}

		eat( Scanner.TokenType.RPAREN, "expected )" );

		return ret;
	}

	private OLSyntaxNode parseOperationExpressionParameter()
		throws IOException, ParserException
	{
		OLSyntaxNode ret = null;

		eat(
			Scanner.TokenType.LPAREN, "expected (" );
		if ( token.isNot( Scanner.TokenType.RPAREN ) ) {
			ret = parseExpression();
		}

		eat( Scanner.TokenType.RPAREN, "expected )" );

		return ret;
	}

	private OLSyntaxNode parseOutputOperationStatement( String id )
		throws IOException, ParserException
	{
		ParsingContext context = getContext();
		String outputPortId = token.content();
		getToken();

		OLSyntaxNode outputExpression = parseOperationExpressionParameter();

		OLSyntaxNode stm;

		if ( token.is( Scanner.TokenType.LPAREN ) ) { // Solicit Response operation
			VariablePathNode inputVarPath = parseOperationVariablePathParameter();
			InstallFunctionNode function = null;
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				eat( Scanner.TokenType.LSQUARE, "expected [" );
				function =
					parseInstallFunction();
				eat(
					Scanner.TokenType.RSQUARE, "expected ]" );
			}

			stm = new SolicitResponseOperationStatement(
				getContext(),
				id,
				outputPortId,
				outputExpression,
				inputVarPath,
				function );
		} else { // Notification operation
			stm = new NotificationOperationStatement( context, id, outputPortId, outputExpression );
		}

		return stm;
	}

	private OLSyntaxNode parseWhileStatement()
		throws IOException, ParserException
	{
		ParsingContext context = getContext();
		OLSyntaxNode cond, process;
		getToken();

		eat(
			Scanner.TokenType.LPAREN, "expected (" );
		cond =
			parseCondition();
		eat(
			Scanner.TokenType.RPAREN, "expected )" );
		eat(
			Scanner.TokenType.LCURLY, "expected {" );
		process =
			parseProcess();
		eat(
			Scanner.TokenType.RCURLY, "expected }" );
		return new WhileStatement( context, cond, process );
	}

	private OLSyntaxNode parseCondition()
		throws IOException, ParserException
	{
		OrConditionNode orCond = new OrConditionNode( getContext() );
		orCond.addChild( parseAndCondition() );
		while ( token.is( Scanner.TokenType.OR ) ) {
			getToken();
			orCond.addChild( parseAndCondition() );
		}

		return orCond;
	}

	private OLSyntaxNode parseAndCondition()
		throws IOException, ParserException
	{
		AndConditionNode andCond = new AndConditionNode( getContext() );
		andCond.addChild( parseBasicCondition() );
		while ( token.is( Scanner.TokenType.AND ) ) {
			getToken();
			andCond.addChild( parseBasicCondition() );
		}

		return andCond;
	}

	private OLSyntaxNode parseNotCondition()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;

		if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal =
				parseCondition();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else {
			retVal = new ExpressionConditionNode( getContext(), parseExpression() );
		}

		return new NotConditionNode( getContext(), retVal );
	}

	private OLSyntaxNode parseBasicCondition()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;

		if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal =
				parseCondition();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.NOT ) ) {
			getToken();
			retVal =
				parseNotCondition();
		} else {
			Scanner.TokenType opType;
			OLSyntaxNode expr1;

			expr1 =
				parseExpression();

			opType =
				token.type();
			if ( opType != Scanner.TokenType.EQUAL && opType != Scanner.TokenType.LANGLE &&
				opType != Scanner.TokenType.RANGLE && opType != Scanner.TokenType.MAJOR_OR_EQUAL &&
				opType != Scanner.TokenType.MINOR_OR_EQUAL && opType != Scanner.TokenType.NOT_EQUAL ) {

				retVal = new ExpressionConditionNode( getContext(), expr1 );
			} else {
				OLSyntaxNode expr2;
				getToken();

				expr2 =
					parseExpression();

				retVal =
					new CompareConditionNode( getContext(), expr1, expr2, opType );
			}

		}

		if ( retVal == null ) {
			throwException( "expected condition" );
		}

		return retVal;
	}

	/**
	 * @todo Check if negative integer handling is appropriate
	 */
	private OLSyntaxNode parseExpression()
		throws IOException, ParserException
	{
		boolean keepRun = true;
		SumExpressionNode sum = new SumExpressionNode( getContext() );
		sum.add( parseProductExpression() );

		while ( keepRun ) {
			if ( token.is( Scanner.TokenType.PLUS ) ) {
				getToken();
				sum.add( parseProductExpression() );
			} else if ( token.is( Scanner.TokenType.MINUS ) ) {
				getToken();
				sum.subtract( parseProductExpression() );
			} else if ( token.is( Scanner.TokenType.INT ) ) { // e.g. i -1
				int value = Integer.parseInt( token.content() );
				// We add it, because it's already negative.
				if ( value < 0 ) {
					sum.add( parseProductExpression() );
				} else // e.g. i 1
				{
					throwException( "expected expression operator" );
				}

			} else if ( token.is( Scanner.TokenType.REAL ) ) { // e.g. i -1
				double value = Double.parseDouble( token.content() );
				// We add it, because it's already negative.
				if ( value < 0 ) {
					sum.add( parseProductExpression() );
				} else // e.g. i 1
				{
					throwException( "expected expression operator" );
				}

			} else {
				keepRun = false;
			}

		}

		return sum;
	}

	private Collection<String> parseIdListN( boolean enclosed )
		throws IOException, ParserException
	{
		Vector<String> idList = new Vector<String>();
		if ( enclosed ) {
			eat( Scanner.TokenType.LPAREN, "expected (" );
		}

		boolean keepRun = true;
		while ( token.is( Scanner.TokenType.ID ) && keepRun ) {
			idList.add( token.content() );
			getToken();

			if ( token.is( Scanner.TokenType.COMMA ) ) {
				getToken();
			} else {
				keepRun = false;
			}

		}
		if ( enclosed ) {
			eat( Scanner.TokenType.RPAREN, "expected )" );
		}

		return idList;
	}

	private OLSyntaxNode parseFactor()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;
		VariablePathNode path = null;

		checkConstant();

		if ( token.is( Scanner.TokenType.ID ) ) {
			String varId = token.content();
			getToken();

			path =
				parseVariablePath( varId );
		} else if ( token.is( Scanner.TokenType.DOT ) ) {
			path = parsePrefixedVariablePath();
		} else if ( insideInstallFunction && token.is( Scanner.TokenType.CARET ) ) {
			getToken();
			String varId = token.content();
			getToken();

			path =
				parseVariablePath( varId );
			retVal =
				new InstallFixedVariableExpressionNode( getContext(), path );

			return retVal;
		}

		if ( path != null ) {
			if ( token.is( Scanner.TokenType.CHOICE ) ) { // Post increment
				getToken();
				retVal =
					new PostIncrementStatement( getContext(), path );
			} else if ( token.is( Scanner.TokenType.DECREMENT ) ) {
				getToken();
				retVal =
					new PostDecrementStatement( getContext(), path );
			} else if ( token.is( Scanner.TokenType.ASSIGN ) ) {
				getToken();
				retVal =
					new AssignStatement( getContext(), path, parseExpression() );
			} else {
				retVal = new VariableExpressionNode( getContext(), path );
			}

		} else if ( token.is( Scanner.TokenType.STRING ) ) {
			retVal = new ConstantStringExpression( getContext(), token.content() );
			getToken();

		} else if ( token.is( Scanner.TokenType.INT ) ) {
			retVal = new ConstantIntegerExpression( getContext(), Integer.parseInt( token.content() ) );
			getToken();

		} else if ( token.is( Scanner.TokenType.REAL ) ) {
			retVal = new ConstantRealExpression( getContext(), Double.parseDouble( token.content() ) );
			getToken();

		} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal =
				parseExpression();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.HASH ) ) {
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new ValueVectorSizeExpressionNode( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.CHOICE ) ) { // Pre increment: ++i
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new PreIncrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) { // Pre decrement
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new PreDecrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.IS_DEFINED ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new IsTypeExpressionNode(
				getContext(), IsTypeExpressionNode.CheckType.DEFINED, parseVariablePath( varId ) );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_INT ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new IsTypeExpressionNode(
				getContext(), IsTypeExpressionNode.CheckType.INT, parseVariablePath( varId ) );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_REAL ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new IsTypeExpressionNode(
				getContext(), IsTypeExpressionNode.CheckType.REAL, parseVariablePath( varId ) );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_STRING ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal =
				new IsTypeExpressionNode(
				getContext(), IsTypeExpressionNode.CheckType.STRING, parseVariablePath( varId ) );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_INT ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			retVal =
				new TypeCastExpressionNode(
				getContext(), Constants.ValueType.INT, parseExpression() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_REAL ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			retVal =
				new TypeCastExpressionNode(
				getContext(), Constants.ValueType.DOUBLE, parseExpression() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_STRING ) ) {
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			retVal =
				new TypeCastExpressionNode(
				getContext(), Constants.ValueType.STRING, parseExpression() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
		}

		if ( retVal == null ) {
			throwException( "expected expression" );
		}

		return retVal;
	}

	private OLSyntaxNode parseProductExpression()
		throws IOException, ParserException
	{
		ProductExpressionNode product = new ProductExpressionNode( getContext() );
		product.multiply( parseFactor() );
		boolean keepRun = true;
		while ( keepRun ) {
			if ( token.is( Scanner.TokenType.ASTERISK ) ) {
				getToken();
				product.multiply( parseFactor() );
			} else if ( token.is( Scanner.TokenType.DIVIDE ) ) {
				getToken();
				product.divide( parseFactor() );
			} else if ( token.is( Scanner.TokenType.PERCENT_SIGN ) ) {
				getToken();
				product.modulo( parseFactor() );
			} else {
				keepRun = false;
			}

		}

		return product;
	}
}
