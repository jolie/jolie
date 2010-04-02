/***************************************************************************
 *   Copyright (C) 2006-2009 by Fabrizio Montesi                           *
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jolie.lang.Constants;
import jolie.lang.NativeType;
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
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OperationCollector;
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
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.util.Pair;
import jolie.util.Range;

/** Parser for a .ol file. 
 * @author Fabrizio Montesi
 *
 */
public class OLParser extends AbstractParser
{
	private final Program program = new Program( new ParsingContext() );
	private final Map< String, Scanner.Token > constantsMap =
		new HashMap< String, Scanner.Token >();
	private boolean insideInstallFunction = false;
	private String[] includePaths;
	private final Map< String, InterfaceDefinition > interfaces =
		new HashMap< String, InterfaceDefinition >();

	private final Map< String, TypeDefinition > definedTypes = createTypeDeclarationMap();
	
	private final ClassLoader classLoader;

	public OLParser( Scanner scanner, String[] includePaths, ClassLoader classLoader )
	{
		super( scanner );
		this.includePaths = includePaths;
		this.classLoader = classLoader;
	}

	public void putConstants( Map< String, Scanner.Token > constantsToPut )
	{
		constantsMap.putAll( constantsToPut );
	}

	static public Map< String, TypeDefinition > createTypeDeclarationMap()
	{
		Map< String, TypeDefinition > definedTypes = new HashMap< String, TypeDefinition >();

		// Fill in defineTypes with all the supported native types (string, int, double, ...)
		for( NativeType type : NativeType.values() ) {
			definedTypes.put( type.id(), new TypeInlineDefinition( new ParsingContext(), type.id(), type, Constants.RANGE_ONE_TO_ONE ) );
		}
		definedTypes.put( TypeDefinitionUndefined.UNDEFINED_KEYWORD, TypeDefinitionUndefined.getInstance() );
		
		return definedTypes;
	}

	public Program parse()
		throws IOException, ParserException
	{
		_parse();

		if ( initSequence != null ) {
			program.addChild( new DefinitionNode( getContext(), "init", initSequence ) );
		}

		program.addChild( main );

		return program;
	}

	private void _parse()
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
			parseTypes();
			parseInclude();
			parseInterfaces();
			parseInclude();
			parsePorts();
			parseInclude();
			parseEmbedded();
			parseInclude();
			parseCode();
		} while( t != token );

		if ( t.isNot( Scanner.TokenType.EOF ) ) {
			throwException( "Invalid token encountered" );
		}
	}

	private void parseTypes()
		throws IOException, ParserException
	{
		String typeName;
		TypeDefinition currentType;

		while( token.isKeyword( "type" ) ) {
			getToken();

			typeName = token.content();
			eat( Scanner.TokenType.ID, "expected type name" );
			eat( Scanner.TokenType.COLON, "expected COLON (cardinality not allowed in root type declaration, it is fixed to [1,1])" );

			NativeType nativeType = readNativeType();
			if ( nativeType == null ) { // It's a user-defined type
				currentType = new TypeDefinitionLink( getContext(), typeName, Constants.RANGE_ONE_TO_ONE, definedTypes.get( token.content() ) );
				getToken();
			} else {
				currentType = new TypeInlineDefinition( getContext(), typeName, nativeType, Constants.RANGE_ONE_TO_ONE );
				getToken();
				if ( token.is( Scanner.TokenType.LCURLY ) ) { // We have sub-types to parse
					parseSubTypes( (TypeInlineDefinition)currentType );
				}
			}

			// Keep track of the root types to support them in successive type declarations
			definedTypes.put( typeName, currentType );
			program.addChild( currentType );
		}
	}

	private NativeType readNativeType()
	{
		if ( token.is( Scanner.TokenType.CAST_INT ) ) {
			return NativeType.INT;
		} else if ( token.is( Scanner.TokenType.CAST_REAL ) ) {
			return NativeType.DOUBLE;
		} else if ( token.is( Scanner.TokenType.CAST_STRING ) ) {
			return NativeType.STRING;
		} else {
			return NativeType.fromString( token.content() );
		}
	}

	private void parseSubTypes( TypeInlineDefinition type )
		throws IOException, ParserException
	{
		eat( Scanner.TokenType.LCURLY, "expected {" );
		
		if ( token.is( Scanner.TokenType.QUESTION_MARK ) ) {
			type.setUntypedSubTypes( true );
			getToken();
		} else {
			TypeDefinition currentSubType;
			while( !token.is( Scanner.TokenType.RCURLY ) ) {
				currentSubType = parseSubType();
				if ( type.hasSubType( currentSubType.id() ) ) {
					throwException( "sub-type " + currentSubType.id() + " conflicts with another sub-type with the same name" );
				}
				type.putSubType( currentSubType );
			}
		}

		eat( Scanner.TokenType.RCURLY, "RCURLY expected" );
	}

	private TypeDefinition parseSubType()
		throws IOException, ParserException
	{
		eat( Scanner.TokenType.DOT, "sub-type syntax error (dot not found)" );

		// SubType id
		String id = token.content();
		eat( Scanner.TokenType.ID, "expected type name" );

		Range cardinality = parseCardinality();
		eat( Scanner.TokenType.COLON, "expected COLON" );

		NativeType nativeType = readNativeType();

		if ( nativeType == null ) { // It's a user-defined type
			TypeDefinitionLink linkedSubType;
			linkedSubType = new TypeDefinitionLink( getContext(), id, cardinality, definedTypes.get( token.content() ) );
			getToken();
			return linkedSubType;
		} else {
			getToken();
			TypeInlineDefinition inlineSubType = new TypeInlineDefinition( getContext(), id, nativeType, cardinality );
			if ( token.is( Scanner.TokenType.LCURLY ) ) { // Has ulterior sub-types
				parseSubTypes( inlineSubType );
			}
			return inlineSubType;
		}
	}

	private Range parseCardinality()
		throws IOException, ParserException
	{
		int min = -1;
		int max = -1;

		if ( token.is( Scanner.TokenType.COLON ) ) { // Default (no cardinality specified)
			min = 1;
			max = 1;
		} else if ( token.is( Scanner.TokenType.QUESTION_MARK ) ) {
			min = 0;
			max = 1;
			getToken();
		} else if ( token.is( Scanner.TokenType.ASTERISK ) ) {
			min = 0;
			max = Integer.MAX_VALUE;
			getToken();
		} else if ( token.is( Scanner.TokenType.LSQUARE ) ) {
			getToken(); // eat [

			// Minimum
			assertToken( Scanner.TokenType.INT, "expected int value" );
			min = Integer.parseInt( token.content() );
			if ( min < 0 ) {
				throwException( "Minimum number of occurences of a sub-type must be positive or zero" );
			}

			getToken();
			eat( Scanner.TokenType.COMMA, "expected comma separator" );

			// Maximum
			if ( token.is( Scanner.TokenType.INT ) ) {
				max = Integer.parseInt( token.content() );
				if ( max < 1 ) {
					throwException( "Maximum number of occurences of a sub-type must be positive" );
				}
			} else if ( token.is( Scanner.TokenType.ASTERISK ) ) {
				max = Integer.MAX_VALUE;
			} else {
				throwException( "Maximum number of sub-type occurences not valid: " + token.content() );
			}

			getToken();
			eat( Scanner.TokenType.RSQUARE, "expected ]" );
		} else {
			throwException( "Sub-type cardinality syntax error" );
		}

		return new Range( min, max );
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
				list = new LinkedList<VariablePathNode>();
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
				if ( token.isValidConstant() == false ) {
					throwException( "expected string, integer, double or identifier constant" );
				}
				if ( constantsMap.containsKey( cId ) == false ) {
					constantsMap.put( cId, token );
				}
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

	private static class IncludeFile {
		private final InputStream inputStream;
		private final String parentPath;
		private IncludeFile( InputStream inputStream, String parentPath )
		{
			this.inputStream = inputStream;
			this.parentPath = parentPath;
		}

		private InputStream getInputStream()
		{
			return inputStream;
		}

		private String getParentPath()
		{
			return parentPath;
		}
	}

	private static IncludeFile retrieveIncludeFile( String path, String filename )
	{
		IncludeFile ret = null;

		File f = new File(
				new StringBuilder()
					.append( path )
					.append( Constants.fileSeparator )
					.append( filename )
					.toString()
				);
		try {
			ret = new IncludeFile(
					new BufferedInputStream( new FileInputStream( f ) ),
					f.getParent()
				);
		} catch( FileNotFoundException e ) {
			try {
				String urlStr =
					new StringBuilder()
						.append( path )
						.append( filename )
						.toString();
				URL url = null;
				if ( urlStr.startsWith( "jap:" ) || urlStr.startsWith( "jar:" ) ) {
					/*
					 * We need the embedded URL path, otherwise URI.normalize
					 * is going to do nothing.
					 */
					url = new URL(
						urlStr.substring( 0,4 ) + new URI( urlStr.substring( 4 ) ).normalize().toString()
					);
				} else {
					url = new URL( new URI( urlStr ).normalize().toString() );
				}
				ret = new IncludeFile(
					url.openStream(),
					path
				);
			} catch( MalformedURLException mue ) {
			} catch( IOException ioe ) {
			} catch( URISyntaxException use ) {}
		}
		return ret;
	}

	private void parseInclude()
		throws IOException, ParserException
	{
		String[] origIncludePaths;
		IncludeFile includeFile;
		while ( token.is( Scanner.TokenType.INCLUDE ) ) {
			getToken();
			Scanner oldScanner = scanner();
			assertToken( Scanner.TokenType.STRING, "expected filename to include" );
			String includeStr = token.content();
			includeFile = null;

			// Try the same directory of the program file first.
			if ( includePaths.length > 1 ) {
				includeFile = retrieveIncludeFile( includePaths[0], includeStr );
			}

			if ( includeFile == null ) {
				URL includeURL = classLoader.getResource( includeStr );
				if ( includeURL != null ) {
					includeFile = new IncludeFile( includeURL.openStream(), null );
				}
			}

			for ( int i = 1; i < includePaths.length && includeFile == null; i++ ) {
				includeFile = retrieveIncludeFile( includePaths[i], includeStr );
			}

			if ( includeFile == null ) {
				throwException( "File not found: " + includeStr );
			}

			origIncludePaths = includePaths;
			setScanner( new Scanner( includeFile.getInputStream(), includeStr ) );

			if ( includeFile.getParentPath() == null ) {
				includePaths = Arrays.copyOf( origIncludePaths, origIncludePaths.length );
			} else {
				includePaths = Arrays.copyOf( origIncludePaths, origIncludePaths.length + 1 );
				includePaths[ origIncludePaths.length ] = includeFile.getParentPath();
			}
			_parse();
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
		OLSyntaxNode protocolConfiguration = new NullProcessStatement( getContext() );

		getToken();
		assertToken( Scanner.TokenType.ID, "expected inputPort name" );
		inputPortName = token.content();
		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		InterfaceDefinition iface = new InterfaceDefinition( getContext(), "Internal interface for: " + inputPortName );
		inputPortLocation = null;
		protocolId = null;
		Map<String, String> redirectionMap = new HashMap<String, String>();
		List< String > aggregationList = new LinkedList< String >();
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
					InterfaceDefinition i = interfaces.get( token.content() );
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
			} else if ( token.isKeyword( "Aggregates" ) ) {
				getToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				while ( token.is( Scanner.TokenType.ID ) ) {
					aggregationList.add( token.content() );
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
		} else if ( iface.operationsMap().isEmpty() && redirectionMap.isEmpty() ) {
			throwException( "expected at least one operation, interface or redirection for service " + inputPortName );
		} else if ( protocolId == null && !inputPortLocation.toString().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			throwException( "expected protocol for inputPort " + inputPortName );
		}
		InputPortInfo iport = new InputPortInfo( getContext(), inputPortName, inputPortLocation, protocolId, protocolConfiguration, aggregationList.toArray( new String[ aggregationList.size() ] ), redirectionMap );
		iface.copyTo( iport );
		program.addChild( iport );
	}

	private void parseInterfaces()
		throws IOException, ParserException
	{
		String name;
		InterfaceDefinition iface;
		while ( token.isKeyword( "interface" ) ) {
			getToken();
			assertToken( Scanner.TokenType.ID, "expected interface name" );
			name = token.content();
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			iface = new InterfaceDefinition( getContext(), name );
			parseOperations( iface );
			interfaces.put( name, iface );
			program.addChild( iface );
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
					InterfaceDefinition i = interfaces.get( token.content() );
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
		String opId;
		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				OneWayOperationDeclaration opDecl = new OneWayOperationDeclaration( getContext(), opId );
				getToken();

				if ( token.is( Scanner.TokenType.LPAREN ) ) { // Type declaration
					getToken(); //eat (
					if ( definedTypes.containsKey( token.content() ) == false ) {
						throwException( "invalid type: " + token.content() );
					}
					opDecl.setRequestType( definedTypes.get( token.content() ) );
					getToken(); // eat the type name
					eat( Scanner.TokenType.RPAREN, "expected )" );
				}

				oc.addOperation( opDecl );

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

		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				getToken();
				String requestTypeName = TypeDefinitionUndefined.UNDEFINED_KEYWORD;
				String responseTypeName = TypeDefinitionUndefined.UNDEFINED_KEYWORD;

				if ( token.is( Scanner.TokenType.LPAREN ) ) {
					getToken(); //eat (
					requestTypeName = token.content();
					getToken();
					eat( Scanner.TokenType.RPAREN, "expected )" );
					eat( Scanner.TokenType.LPAREN, "expected (" );
					responseTypeName = token.content();
					getToken();
					eat( Scanner.TokenType.RPAREN, "expected )" );
				}

				Map< String, TypeDefinition > faultTypesMap = new HashMap< String, TypeDefinition >();

				if ( token.is( Scanner.TokenType.THROWS ) ) {
					getToken();
					while( token.is( Scanner.TokenType.ID ) ) {
						String faultName = token.content();
						String faultTypeName = TypeDefinitionUndefined.UNDEFINED_KEYWORD;
						getToken();
						if ( token.is( Scanner.TokenType.LPAREN ) ) {
							getToken(); //eat (
							faultTypeName = token.content();
							getToken();
							eat( Scanner.TokenType.RPAREN, "expected )" );
						}
						faultTypesMap.put( faultName, definedTypes.get( faultTypeName ) );
					}
				}

				if ( requestTypeName != null && definedTypes.containsKey( requestTypeName ) == false ) {
					throwException( "invalid type: " + requestTypeName );
				}

				if ( responseTypeName != null && definedTypes.containsKey( responseTypeName ) == false ) {
					throwException( "invalid type: " + requestTypeName );
				}

				RequestResponseOperationDeclaration opRR =
					new RequestResponseOperationDeclaration(
						getContext(),
						opId,
						definedTypes.get( requestTypeName ),
						definedTypes.get( responseTypeName ),
						faultTypesMap
					);

				oc.addOperation( opRR );
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
	private DefinitionNode main = null;

	private void parseCode()
		throws IOException, ParserException
	{
		boolean keepRun = true;

		do {
			if ( token.is( Scanner.TokenType.DEFINE ) ) {
				program.addChild( parseDefinition() );
			} else if ( token.isKeyword( "main" ) ) {
				if ( main != null ) {
					throwException( "you must specify only one main definition" );
				}

				main = parseMain();
			} else if ( token.is( Scanner.TokenType.INIT ) ) {
				if ( initSequence == null ) {
					initSequence = new SequenceStatement( getContext() );
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
		eat( Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		DefinitionNode retVal = new DefinitionNode( getContext(), "main", parseProcess() );
		eat( Scanner.TokenType.RCURLY, "expected } after procedure definition" );
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
	private List< List< Scanner.Token > > inVariablePaths = new LinkedList< List< Scanner.Token > >();

	private OLSyntaxNode parseInVariablePathProcess(
		boolean withConstruct )
		throws IOException, ParserException
	{
		OLSyntaxNode ret = null;
		List< Scanner.Token > tokens = new LinkedList< Scanner.Token >();

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
		List< Pair< String, OLSyntaxNode > > vec =
			new LinkedList< Pair< String, OLSyntaxNode > >();

		boolean keepRun = true;
		List< String > names = new LinkedList< String >();
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
		return new InstallFunctionNode( vec.toArray( new Pair[ vec.size() ] ) );
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
		List< Scanner.Token > tokens = new ArrayList< Scanner.Token >();
		tokens.addAll( inVariablePaths.get( i ) );

		while ( tokens.get( 0 ).is( Scanner.TokenType.DOT ) ) {
			i--;
			tokens.addAll( 0, inVariablePaths.get( i ) );
		}

		addTokens( tokens );
		addTokens( Arrays.asList( new Scanner.Token( Scanner.TokenType.DOT ) ) );
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
				} else { // e.g. i 1
					throwException( "expected expression operator" );
				}
			} else if ( token.is( Scanner.TokenType.REAL ) ) { // e.g. i -1
				double value = Double.parseDouble( token.content() );
				// We add it, because it's already negative.
				if ( value < 0 ) {
					sum.add( parseProductExpression() );
				} else { // e.g. i 1
					throwException( "expected expression operator" );
				}
			} else {
				keepRun = false;
			}
		}

		return sum;
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
			path = parseVariablePath( varId );
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
			retVal = parseExpression();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.HASH ) ) {
			getToken();
			assertToken( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new ValueVectorSizeExpressionNode(
				getContext(),
				parseVariablePath( varId )
			);
		} else if ( token.is( Scanner.TokenType.CHOICE ) ) { // Pre increment: ++i
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new PreIncrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) { // Pre decrement
			getToken();
			assertToken(
				Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new PreDecrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.IS_DEFINED ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			assertToken( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new IsTypeExpressionNode(
				getContext(),
				IsTypeExpressionNode.CheckType.DEFINED,
				parseVariablePath( varId )
			);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_INT ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			assertToken( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal = new IsTypeExpressionNode(
				getContext(),
				IsTypeExpressionNode.CheckType.INT,
				parseVariablePath( varId )
			);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_REAL ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			assertToken( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal = new IsTypeExpressionNode(
				getContext(),
				IsTypeExpressionNode.CheckType.REAL,
				parseVariablePath( varId )
			);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_STRING ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			assertToken( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();

			retVal = new IsTypeExpressionNode(
				getContext(),
				IsTypeExpressionNode.CheckType.STRING,
				parseVariablePath( varId )
			);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_INT ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new TypeCastExpressionNode( getContext(), NativeType.INT, parseExpression() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_REAL ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new TypeCastExpressionNode(
				getContext(),
				NativeType.DOUBLE,
				parseExpression()
			);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_STRING ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new TypeCastExpressionNode(
				getContext(),
				NativeType.STRING,
				parseExpression()
			);
			eat( Scanner.TokenType.RPAREN, "expected )" );
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
