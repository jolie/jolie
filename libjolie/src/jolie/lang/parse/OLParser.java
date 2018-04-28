/*
 * Copyright (C) 2006-2016 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.lang.parse;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jolie.lang.Constants;
import jolie.lang.NativeType;
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
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallFunctionNode;
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
import jolie.lang.parse.ast.OperationCollector;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ProvideUntilStatement;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.Scope;
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
import jolie.lang.parse.ast.VariablePathNode.Type;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
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
import jolie.lang.parse.context.URIParsingContext;
import jolie.util.Helpers;
import jolie.util.Pair;
import jolie.util.Range;

/** Parser for a .ol file.
 * @author Fabrizio Montesi
 *
 */
public class OLParser extends AbstractParser
{
	private final Program program;
	private final Map< String, Scanner.Token > constantsMap =
		new HashMap<>();
	private boolean insideInstallFunction = false;
	private String[] includePaths;
	private final Map< String, InterfaceDefinition > interfaces =
		new HashMap<>();
	private final Map< String, InterfaceExtenderDefinition > interfaceExtenders =
		new HashMap<>();

	private final Map< String, TypeDefinition > definedTypes;
	private final ClassLoader classLoader;

	private InterfaceExtenderDefinition currInterfaceExtender = null;

	public OLParser( Scanner scanner, String[] includePaths, ClassLoader classLoader )
	{
		super( scanner );
		ParsingContext context = new URIParsingContext( scanner.source(), 0 );
		this.program = new Program( context );
		this.includePaths = includePaths;
		this.classLoader = classLoader;
		this.definedTypes = createTypeDeclarationMap( context );
	}

	public void putConstants( Map< String, Scanner.Token > constantsToPut )
	{
		constantsMap.putAll( constantsToPut );
	}

	public static Map< String, TypeDefinition > createTypeDeclarationMap( ParsingContext context )
	{
		Map< String, TypeDefinition > definedTypes = new HashMap<>();

		// Fill in defineTypes with all the supported native types (string, int, double, ...)
		for( NativeType type : NativeType.values() ) {
			definedTypes.put( type.id(), new TypeInlineDefinition( context, type.id(), type, Constants.RANGE_ONE_TO_ONE ) );
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

		if ( main != null ) {
			program.addChild( main );
		}
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
			parseCorrelationSets();
			parseInclude();
            parseTypes();
			parseInclude();
			parseInterfaceOrPort();
			parseInclude();
			parseEmbedded();
			parseInclude();
            parseInternalService();
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
		Scanner.Token commentToken = new Scanner.Token( Scanner.TokenType.DOCUMENTATION_COMMENT, "" );
		boolean keepRun = true;
		boolean haveComment = false;
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.DOCUMENTATION_COMMENT ) ) {
				haveComment = true;
				commentToken = token;
				getToken();
			} else if ( token.isKeyword( "type" ) ) {
				String typeName;
				TypeDefinition currentType;

				getToken();
				typeName = token.content();
				eat( Scanner.TokenType.ID, "expected type name" );
				eat( Scanner.TokenType.COLON, "expected COLON (cardinality not allowed in root type declaration, it is fixed to [1,1])" );

				currentType = parseType( typeName );
				typeName = currentType.id();

				definedTypes.put( typeName, currentType );
				program.addChild( currentType );
			} else {
				keepRun = false;
				if ( haveComment ) {
					addToken( commentToken );
					addToken( token );
					getToken();
				}
			}
		}
	}

	private TypeDefinition parseType( String typeName )
			throws IOException, ParserException
	{
		TypeDefinition currentType;

		NativeType nativeType = readNativeType();
		if ( nativeType == null ) { // It's a user-defined type
			currentType = new TypeDefinitionLink( getContext(), typeName, Constants.RANGE_ONE_TO_ONE, token.content() );
			getToken();
		} else {
			currentType = new TypeInlineDefinition( getContext(), typeName, nativeType, Constants.RANGE_ONE_TO_ONE );
			getToken();
			if ( token.is( Scanner.TokenType.LCURLY ) ) { // We have sub-types to parse
				parseSubTypes( (TypeInlineDefinition) currentType );
			}
		}

		if ( token.is( Scanner.TokenType.PARALLEL ) ) { // It's a sum (union, choice) type
			getToken();
			final TypeDefinition secondType = parseType( typeName );
			final TypeChoiceDefinition choiceDefinition = new TypeChoiceDefinition( getContext(), typeName, Constants.RANGE_ONE_TO_ONE, currentType, secondType );
			return choiceDefinition;
		}

		return currentType;
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
				eat( Scanner.TokenType.DOT, "sub-type syntax error (dot not found)" );

				// SubType id
				String id = token.content();
				if ( token.is( Scanner.TokenType.STRING ) ) {
					getToken();
				} else {
					eatIdentifier( "expected type name" );
				}
				
				Range cardinality = parseCardinality();
				eat( Scanner.TokenType.COLON, "expected COLON" );

				currentSubType = parseSubType(id, cardinality);
				if ( type.hasSubType( currentSubType.id() ) ) {
					throwException( "sub-type " + currentSubType.id() + " conflicts with another sub-type with the same name" );
				}
				type.putSubType( currentSubType );
			}
		}

		eat( Scanner.TokenType.RCURLY, "RCURLY expected" );
	}

	private TypeDefinition parseSubType(String id, Range cardinality)
			throws IOException, ParserException
	{
		NativeType nativeType = readNativeType();
		TypeDefinition subType;
		// SubType id

		if ( nativeType == null ) { // It's a user-defined type
			subType = new TypeDefinitionLink( getContext(), id, cardinality, token.content() );
			getToken();
		} else {
			getToken();
			subType = new TypeInlineDefinition( getContext(), id, nativeType, cardinality );
			if ( token.is( Scanner.TokenType.LCURLY ) ) { // Has ulterior sub-types
				parseSubTypes((TypeInlineDefinition) subType);
			}
		}

		if ( token.is( Scanner.TokenType.PARALLEL ) ) {
			getToken();
			TypeDefinition secondSubType = parseSubType(id, cardinality);
			TypeChoiceDefinition choiceSubType = new TypeChoiceDefinition( getContext(), id, cardinality, subType, secondSubType);
			return choiceSubType;
		}
		return subType;
	}

	private NativeType readNativeType()
	{
		if ( token.is( Scanner.TokenType.CAST_INT ) ) {
			return NativeType.INT;
		} else if ( token.is( Scanner.TokenType.CAST_DOUBLE ) ) {
			return NativeType.DOUBLE;
		} else if ( token.is( Scanner.TokenType.CAST_STRING ) ) {
			return NativeType.STRING;
		} else if ( token.is( Scanner.TokenType.CAST_LONG ) ) {
			return NativeType.LONG;
		} else if ( token.is( Scanner.TokenType.CAST_BOOL ) ) {
			return NativeType.BOOL;
		} else {
			return NativeType.fromString( token.content() );
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

	private void parseCorrelationSets()
		throws IOException, ParserException
	{
		while( token.isKeyword( "cset" ) ) {
			getToken();
			/*assertToken( Scanner.TokenType.ID, "expected correlation set name" );
			String csetName = token.content();
			getToken();*/
			eat( Scanner.TokenType.LCURLY, "expected {" );
			List< CorrelationVariableInfo > variables = new LinkedList<>();
			List< CorrelationAliasInfo > aliases;
			VariablePathNode correlationVariablePath;
			String typeName;
			while ( token.is( Scanner.TokenType.ID ) ) {
				aliases = new LinkedList<>();
				correlationVariablePath = parseVariablePath();
				eat( Scanner.TokenType.COLON, "expected correlation variable alias list" );
				assertToken( Scanner.TokenType.ID, "expected correlation variable alias" );
				while ( token.is( Scanner.TokenType.ID ) ) {
					typeName = token.content();
					getToken();
					eat( Scanner.TokenType.DOT, "expected . after message type name in correlation alias" );
					aliases.add( new CorrelationAliasInfo( typeName, parseVariablePath() ) );
				}
				variables.add( new CorrelationVariableInfo( correlationVariablePath, aliases ) );
				if ( token.is( Scanner.TokenType.COMMA ) ) {
					getToken();
				} else {
					break;
				}
			}

			program.addChild( new CorrelationSetInfo( getContext(), variables ) );
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
			assertToken( Scanner.TokenType.ID, "expected execution modality" );
			switch( token.content() ) {
				case "sequential":
					mode = Constants.ExecutionMode.SEQUENTIAL;
					break;
				case "concurrent":
					mode = Constants.ExecutionMode.CONCURRENT;
					break;
				case "single":
					mode = Constants.ExecutionMode.SINGLE;
					break;
				default:
					throwException( "Expected execution mode, found " + token.content() );
					break;
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
	
	private URL guessIncludeFilepath( String urlStr, String filename, String path )
	{
		try {
			if ( urlStr.startsWith( "jap:" ) || urlStr.startsWith( "jar:" ) ) {
				// Try hard to resolve names, even in Windows
				if ( filename.startsWith( "../" ) ) {
					String tmpPath = path;
					String tmpFilename = filename;
					if ( !tmpPath.contains( "/" ) && tmpPath.contains( "\\" ) ) {
						tmpPath = tmpPath.replace( "\\", "/" );
					}
					while( tmpFilename.startsWith( "../" ) ) {
						tmpFilename = tmpFilename.substring( 2 );
						if ( tmpPath.endsWith( "/" ) ) {
							tmpPath = tmpPath.substring( 0, tmpPath.length() - 1 );
						}
						tmpPath = tmpPath.substring( 0, tmpPath.lastIndexOf( "/" ) );
					}

					String tmpUrl = build( tmpPath, tmpFilename );
					try {
						return new URL( tmpUrl.substring( 0, 4 ) + tmpUrl.substring( 4 ) );
					} catch( Exception exn ) {}
				} else if ( filename.startsWith( "./" ) ) {
					String tmpPath = path;
					String tmpFilename = filename;
					if ( !tmpPath.contains( "/" ) && tmpPath.contains( "\\" ) ) {
						tmpPath = tmpPath.replace( "\\", "/" );
					}
					tmpFilename = tmpFilename.substring( 1 );
					if ( tmpPath.endsWith( "/" ) ) {
						tmpPath = tmpPath.substring( 0, tmpPath.length() - 1 );
					}
					String tmpUrl = build( tmpPath, tmpFilename );
					return new URL( tmpUrl.substring( 0, 4 ) + tmpUrl.substring( 4 ) );
				} else {
					/*
					 * We need the embedded URL path, otherwise URI.normalize
					 * is going to do nothing.
					 */
					return new URL(
						urlStr.substring( 0, 4 ) + new URI( urlStr.substring( 4 ) ).normalize().toString() );
				}
			} else {
				return new URL( new URI( urlStr ).normalize().toString() );
			}
		} catch( MalformedURLException | URISyntaxException e ) {}
		return null;
	}
	
	private IncludeFile retrieveIncludeFile( final String path, final String filename )
	{
		IncludeFile ret;
		
		String urlStr = build( path, Constants.fileSeparator, filename );

		ret = tryAccessIncludeFile( urlStr );
		if ( ret == null ) {
			final URL url = guessIncludeFilepath( urlStr, filename, path );
			if ( url != null ) {
				ret = tryAccessIncludeFile( url.toString() );
			}
		}
		return ret;
	}
	
	private final Map< String, URL > resourceCache = new HashMap<>();
	
	private IncludeFile tryAccessIncludeFile( String includeStr )
	{
		if ( Helpers.getOperatingSystemType() == Helpers.OSType.Windows ) {
			includeStr = includeStr.replace( "\\", "/" );
			if ( includeStr.charAt( 1 ) == ':' ) {
				// Remove the drive name if present
				includeStr = includeStr.substring( 2 );
			}
		}

		final URL includeURL = resourceCache.computeIfAbsent(
			includeStr,
			classLoader::getResource
		);
		
		if ( includeURL != null ) {
			try {
				String parent;
				URI uri;
				try {
					Path path = Paths.get( includeURL.toURI() );
					parent = path.getParent().toString();
					uri = path.toUri();
				} catch( FileSystemNotFoundException e ) {
					parent = null;
					uri = includeURL.toURI();
				}
				return new IncludeFile( new BufferedInputStream( includeURL.openStream() ), parent, uri );
			} catch( IOException | URISyntaxException e ) {
				e.printStackTrace();
			}
		}

		return null;
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
			/* if ( includePaths.length > 1 ) {
				includeFile = retrieveIncludeFile( includePaths[0], includeStr );
			} */

			/* if ( includeFile == null ) {
				URL includeURL = classLoader.getResource( includeStr );
				if ( includeURL != null ) {
					File f = new File( includeURL.toString() );
					includeFile = new IncludeFile( includeURL.openStream(), f.getParent(), f.toURI() );
				}
			} */

			for ( int i = 0; i < includePaths.length && includeFile == null; i++ ) {
				includeFile = retrieveIncludeFile( includePaths[i], includeStr );
			}
			
			if ( includeFile == null ) {
				includeFile = tryAccessIncludeFile( includeStr );
				if ( includeFile == null ) {
					throwException( "File not found: " + includeStr );
				}
			}

			origIncludePaths = includePaths;
			// includes are explicitly parsed in ASCII to be independent of program's encoding
			setScanner( new Scanner( includeFile.getInputStream(), includeFile.getURI(), "US-ASCII" ) );
			
			if ( includeFile.getParentPath() == null ) {
				includePaths = Arrays.copyOf( origIncludePaths, origIncludePaths.length );
			} else {
				includePaths = Arrays.copyOf( origIncludePaths, origIncludePaths.length + 1 );
				includePaths[ origIncludePaths.length ] = includeFile.getParentPath();
			}
			_parse();
			includePaths = origIncludePaths;
			includeFile.getInputStream().close();
			setScanner( oldScanner );
			getToken();
		}
	}

	private boolean checkConstant()
	{
		if ( token.is( Scanner.TokenType.ID ) ) {
			final Scanner.Token t;
			final Constants.Predefined p = Constants.Predefined.get( token.content() );
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

	private PortInfo parsePort()
		throws IOException, ParserException
	{
		PortInfo portInfo = null;
		if ( token.isKeyword( "inputPort" ) ) {
			portInfo = parseInputPortInfo();
		} else if ( token.isKeyword( "outputPort" ) ) {
			getToken();
			assertToken( Scanner.TokenType.ID, "expected output port identifier" );
			OutputPortInfo p = new OutputPortInfo( getContext(), token.content() );
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			parseOutputPortInfo( p );
			program.addChild( p );
			eat( Scanner.TokenType.RCURLY, "expected }" );
			portInfo = p;
		}
		return portInfo;
	}

	private void parseInterfaceOrPort()
		throws IOException, ParserException
	{
		Scanner.Token commentToken = new Scanner.Token( Scanner.TokenType.DOCUMENTATION_COMMENT, "" );
		boolean keepRun = true;
		DocumentedNode node = null;
		boolean haveDocumentation = false;
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.DOCUMENTATION_COMMENT ) ) {
				haveDocumentation = true;
				commentToken = token;
				getToken();
			} else if ( token.isKeyword( "interface" ) ) {
				getToken();
				if ( token.isKeyword( "extender") ) {
					getToken();
					node = parseInterfaceExtender();
				} else {
					node = parseInterface();
				}
			} else if ( token.isKeyword( "inputPort" ) ) {
				node = parsePort();
			} else if ( token.isKeyword( "outputPort" ) ) {
				node = parsePort();
			} else {
				keepRun = false;
				
				if ( haveDocumentation ) {
					addToken( commentToken );
					addToken( token );
					getToken();
				}
			}
			
			if ( haveDocumentation && node != null ) {
				node.setDocumentation( commentToken.content() );
				haveDocumentation = false;
				node = null;
			}
		}
    }

    private OutputPortInfo createInternalServicePort( String name, List< InterfaceDefinition> interfaceList )
		throws ParserException
	{
		OutputPortInfo p = new OutputPortInfo( getContext(), name );

		for( InterfaceDefinition interfaceDefinition : interfaceList ) {
			interfaceDefinition.copyTo( p );
		}
		return p;
	}
    
	private InputPortInfo createInternalServiceInputPort( String serviceName, List< InterfaceDefinition> interfaceList )
		throws ParserException
	{
		OLSyntaxNode protocolConfiguration = new NullProcessStatement( getContext() );
		InputPortInfo iport = null;
		try {
			iport = new InputPortInfo(
				getContext(),
				serviceName + "InputPort", //input port name
				new URI( Constants.LOCAL_LOCATION_KEYWORD ),
				null,
				protocolConfiguration,
				new InputPortInfo.AggregationItemInfo[]{},
				Collections.<String, String>emptyMap() );

			for( InterfaceDefinition i : interfaceList ) {
				i.copyTo( iport );
			}
		} catch( URISyntaxException e ) {
			throwException( e );
		}
		return iport;
	}
    
	/**
	 * Parses an internal service, i.e. service service_name {}
	 *
	 * @throws IOException
	 * @throws ParserException
	 */
	private void parseInternalService()
		throws IOException, ParserException
	{
		//only proceed if a service declaration
		if ( !token.isKeyword( "service" ) ) {
			return;
		}

		//get service name
		getToken();
		assertToken( Scanner.TokenType.ID, "expected service name" );
		String serviceName = token.content();

		//validate token
		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );

		//initialize internal interface and interface list
		List< InterfaceDefinition> interfaceList = new ArrayList<>();

		OLSyntaxNode internalMain = null;
		SequenceStatement internalInit = null;

		boolean keepRunning = true;
		while( keepRunning ) {
			if ( token.isKeyword( "Interfaces" ) ) {
				getToken();
				eat( Scanner.TokenType.COLON, "expected : after Interfaces" );
				boolean keepRun = true;
				while( keepRun ) {
					assertToken( Scanner.TokenType.ID, "expected interface name" );
					InterfaceDefinition i = interfaces.get( token.content() );
					if ( i == null ) {
						throwException( "Invalid interface name: " + token.content() );
					}
					interfaceList.add( i );
					getToken();

					if ( token.is( Scanner.TokenType.COMMA ) ) {
						getToken();
					} else {
						keepRun = false;
					}
				}
			} else if ( token.isKeyword( "main" ) ) {
				if ( internalMain != null ) {
					throwException( "you must specify only one main definition" );
				}

				internalMain = parseMain();
			} else if ( token.is( Scanner.TokenType.INIT ) ) {
				if ( internalInit == null ) {
					internalInit = new SequenceStatement( getContext() );
				}

				internalInit.addChild( parseInit() );
			} else if ( token.is( Scanner.TokenType.RCURLY ) ) {
				keepRunning = false;
			} else {
				throwException( "Unrecognized token in inline service." );
			}
		}

		//validate ending
		eat( Scanner.TokenType.RCURLY, "} expected" );

		//main in service needs to be defined
		if ( internalMain == null ) {
			throwException( "You must specify a main for internal service " + serviceName );
		}

		//add output port to main program
		program.addChild( createInternalServicePort( serviceName, interfaceList ) );
		
		//create Program representing the internal service
		Program internalServiceProgram = new Program( getContext() );

		// copy children of parent to embedded service
		for( OLSyntaxNode child : program.children() ) {
			if ( child instanceof InterfaceDefinition
				|| child instanceof OutputPortInfo
				|| child instanceof TypeDefinition
				|| child instanceof TypeInlineDefinition
				|| child instanceof TypeDefinitionLink
				|| child instanceof TypeDefinitionUndefined ) {
				internalServiceProgram.addChild( child );
			}
		}

		// set execution to always concurrent
		internalServiceProgram.addChild( new ExecutionInfo( getContext(), Constants.ExecutionMode.CONCURRENT ) );

		//add input port to internal service
		internalServiceProgram.addChild( createInternalServiceInputPort( serviceName, interfaceList ) );

		//add init if defined in internal service
		if ( internalInit != null ) {
			internalServiceProgram.addChild( new DefinitionNode( getContext(), "init", internalInit ) );
		}

		//add main defined in internal service
		internalServiceProgram.addChild( internalMain );

		//create internal embedded service node
		EmbeddedServiceNode internalServiceNode
			= new EmbeddedServiceNode( getContext(), Constants.EmbeddedServiceType.INTERNAL, serviceName, serviceName );

		//add internal service program to embedded service node
		internalServiceNode.setProgram( internalServiceProgram );

		//add embedded service node to program that is embedding it
		program.addChild( internalServiceNode );
	}
    
	private InputPortInfo parseInputPortInfo()
		throws IOException, ParserException
	{
		String inputPortName;
		String protocolId;
		URI inputPortLocation;
		List< InterfaceDefinition > interfaceList = new ArrayList<>();
		OLSyntaxNode protocolConfiguration = new NullProcessStatement( getContext() );
		
		getToken();
		assertToken( Scanner.TokenType.ID, "expected inputPort name" );
		inputPortName = token.content();
		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		InterfaceDefinition iface = new InterfaceDefinition( getContext(), "Internal interface for: " + inputPortName );
		
		inputPortLocation = null;
		protocolId = null;
		Map<String, String> redirectionMap = new HashMap<>();
		List< InputPortInfo.AggregationItemInfo > aggregationList = new ArrayList<>();
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
                    interfaceList.add( i );
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
				parseAggregationList( aggregationList );
			} else {
				throwException( "Unrecognized token in inputPort " + inputPortName );
			}
		}
		eat( Scanner.TokenType.RCURLY, "} expected" );
		if ( inputPortLocation == null ) {
			throwException( "expected location URI for " + inputPortName );
		} else if ( iface.operationsMap().isEmpty() && redirectionMap.isEmpty() && aggregationList.isEmpty() ) {
			throwException( "expected at least one operation, interface, aggregation or redirection for inputPort " + inputPortName );
		} else if ( protocolId == null && !inputPortLocation.toString().equals( Constants.LOCAL_LOCATION_KEYWORD ) && !inputPortLocation.getScheme().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			throwException( "expected protocol for inputPort " + inputPortName );
		}
		InputPortInfo iport = new InputPortInfo( getContext(), inputPortName, inputPortLocation, protocolId, protocolConfiguration, aggregationList.toArray( new InputPortInfo.AggregationItemInfo[ aggregationList.size() ] ), redirectionMap );
		for( InterfaceDefinition i : interfaceList ) {
			iport.addInterface( i );
		}
		iface.copyTo( iport );
		program.addChild( iport );
		return iport;
	}
	
	private void parseAggregationList( List< InputPortInfo.AggregationItemInfo > aggregationList )
		throws ParserException, IOException
	{
		List< String > outputPortNames;
		InterfaceExtenderDefinition extender;
		for( boolean mainKeepRun = true; mainKeepRun; ) {
			extender = null;
			outputPortNames = new LinkedList<>();
			if ( token.is( Scanner.TokenType.LCURLY ) ) {
				getToken();
				for( boolean keepRun = true; keepRun; ) {
					assertToken( Scanner.TokenType.ID, "expected output port name" );
					outputPortNames.add( token.content() );
					getToken();
					if ( token.is( Scanner.TokenType.COMMA ) ) {
						getToken();
					} else if ( token.is( Scanner.TokenType.RCURLY ) ) {
						keepRun = false;
						getToken();
					} else {
						throwException( "unexpected token " + token.type() );
					}
				}
			} else {
				assertToken( Scanner.TokenType.ID, "expected output port name" );
				outputPortNames.add( token.content() );
				getToken();
			}
			if ( token.is( Scanner.TokenType.WITH ) ) {
				getToken();
				assertToken( Scanner.TokenType.ID, "expected interface extender name" );
				extender = interfaceExtenders.get( token.content() );
				if ( extender == null ) {
					throwException( "undefined interface extender: " + token.content() );
				}
				getToken();
			}
			aggregationList.add( new InputPortInfo.AggregationItemInfo(
				outputPortNames.toArray( new String[ outputPortNames.size() ] ),
				extender
			) );

			if ( token.is( Scanner.TokenType.COMMA ) ) {
				getToken();
			} else {
				mainKeepRun = false;
			}
		}
	}
	
	private InterfaceDefinition parseInterfaceExtender()
		throws IOException, ParserException
	{
		String name;
		assertToken( Scanner.TokenType.ID, "expected interface extender name" );
		name = token.content();
		getToken();
		eat( Scanner.TokenType.LCURLY, "expected {" );
		InterfaceExtenderDefinition extender = currInterfaceExtender =
				new InterfaceExtenderDefinition( getContext(), name );
		parseOperations( currInterfaceExtender );
		interfaceExtenders.put( name, extender );
		program.addChild( currInterfaceExtender );
		eat( Scanner.TokenType.RCURLY, "expected }" );
		currInterfaceExtender = null;
		return extender;
	}

	private InterfaceDefinition parseInterface()
		throws IOException, ParserException
	{
		String name;
		InterfaceDefinition iface;
        assertToken( Scanner.TokenType.ID, "expected interface name" );
		name = token.content();
		getToken();
		eat( Scanner.TokenType.LCURLY, "expected {" );
		iface = new InterfaceDefinition( getContext(), name );
		parseOperations( iface );
		interfaces.put( name, iface );
		program.addChild( iface );
		eat( Scanner.TokenType.RCURLY, "expected }" );

		return iface;
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
					p.addInterface( i );
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

				assertToken( Scanner.TokenType.STRING, "expected location string" );
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
		boolean commentsPreset = false;
		String comment = "";
		String opId;
		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.DOCUMENTATION_COMMENT ) ) {
				commentsPreset = true;
				comment = token.content();
				getToken();
			} else if ( token.is( Scanner.TokenType.ID ) || (
				currInterfaceExtender != null && token.is( Scanner.TokenType.ASTERISK )
			) ) {
				opId = token.content();
				OneWayOperationDeclaration opDecl = new OneWayOperationDeclaration( getContext(), opId );
				getToken();
				opDecl.setRequestType( TypeDefinitionUndefined.getInstance() );
				if ( token.is( Scanner.TokenType.LPAREN ) ) { // Type declaration
					getToken(); //eat (
					if ( definedTypes.containsKey( token.content() ) == false ) {
						throwException( "invalid type: " + token.content() );
					}
					opDecl.setRequestType( definedTypes.get( token.content() ) );
					getToken(); // eat the type name
					eat( Scanner.TokenType.RPAREN, "expected )" );
				}

				if ( commentsPreset ) {
					opDecl.setDocumentation( comment );
					commentsPreset = false;
				}

				if ( currInterfaceExtender != null && opId.equals( "*" ) ) {
					currInterfaceExtender.setDefaultOneWayOperation( opDecl );
				} else {
					oc.addOperation( opDecl );
				}

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
		String comment = "";
		String opId;
		boolean commentsPreset = false;
		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.DOCUMENTATION_COMMENT ) ) {
				commentsPreset = true;
				comment = token.content();
				getToken();
			} else if ( token.is( Scanner.TokenType.ID ) || (
				currInterfaceExtender != null && token.is( Scanner.TokenType.ASTERISK )
			) ) {
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

				Map< String, TypeDefinition > faultTypesMap = new HashMap<>();

				if ( token.is( Scanner.TokenType.THROWS ) ) {
					getToken();
					while( token.is( Scanner.TokenType.ID ) ) {
						String faultName = token.content();
						String faultTypeName = TypeDefinitionUndefined.UNDEFINED_KEYWORD;
						getToken();
						if ( token.is( Scanner.TokenType.LPAREN ) ) {
							getToken(); //eat (
							faultTypeName = token.content();
							if ( definedTypes.containsKey( faultTypeName ) == false ) {
								throwException( "invalid type: " + faultTypeName );
							}
							getToken();
							eat( Scanner.TokenType.RPAREN, "expected )" );
						}
						faultTypesMap.put( faultName, definedTypes.get( faultTypeName ) );
					}
				}

				if ( definedTypes.containsKey( requestTypeName ) == false ) {
					throwException( "invalid type: " + requestTypeName );
				}

				if ( definedTypes.containsKey( responseTypeName ) == false ) {
					throwException( "invalid type: " + responseTypeName );
				}

				RequestResponseOperationDeclaration opRR =
					new RequestResponseOperationDeclaration(
						getContext(),
						opId,
						definedTypes.get( requestTypeName ),
						definedTypes.get( responseTypeName ),
						faultTypesMap
					);

				// adding documentation
				if ( commentsPreset ) {
					opRR.setDocumentation( comment );
					commentsPreset = false;
				}

				if ( currInterfaceExtender != null && opId.equals( "*" ) ) {
					currInterfaceExtender.setDefaultRequestResponseOperation( opRR );
				} else {
					oc.addOperation( opRR );
				}
				
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
			} else if ( token.isKeyword( "courier" ) ) {
				program.addChild( parseCourierDefinition() );
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
		assertToken( Scanner.TokenType.ID, "expected definition identifier" );
		String definitionId = token.content();
		getToken();

		eat( Scanner.TokenType.LCURLY, "expected { after definition declaration" );
		DefinitionNode retVal =
			new DefinitionNode(
			getContext(),
			definitionId,
			parseProcess() );
		eat( Scanner.TokenType.RCURLY, "expected } after definition declaration" );

		return retVal;
	}
	
	private CourierDefinitionNode parseCourierDefinition()
		throws IOException, ParserException
	{
		getToken();
		assertToken( Scanner.TokenType.ID, "expected input port identifier" );
		String inputPortName = token.content();
		getToken();

		eat( Scanner.TokenType.LCURLY, "expected { after courier definition" );
		CourierDefinitionNode retVal = new CourierDefinitionNode(
			getContext(),
			inputPortName,
			parseCourierChoice()
		);
		eat( Scanner.TokenType.RCURLY, "expected } after courier definition" );

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
	
	private final List< List< Scanner.Token > > inVariablePaths = new ArrayList<>();

	private OLSyntaxNode parseInVariablePathProcess( boolean withConstruct )
		throws IOException, ParserException
	{
		OLSyntaxNode ret;
		LinkedList< Scanner.Token> tokens = new LinkedList<>();

		try {
			if ( withConstruct ) {
				eat( Scanner.TokenType.LPAREN, "expected (" );
				while( token.isNot( Scanner.TokenType.LCURLY ) ) {
					tokens.add( token );
					getTokenNotEOF();
				}
				//TODO transfer this whole buggy thing to the OOIT
				tokens.removeLast();
				//getToken();
			} else {
				while( token.isNot( Scanner.TokenType.LCURLY ) ) {
					tokens.add( token );
					getTokenNotEOF();
				}
			}
		} catch( EOFException eof ) {
			throwException( "with clause requires a { at the beginning of its body" );
		}
		inVariablePaths.add( tokens );

		eat( Scanner.TokenType.LCURLY, "expected {" );
		ret = parseProcess();
		eat( Scanner.TokenType.RCURLY, "expected }" );
		inVariablePaths.remove( inVariablePaths.size() - 1 );

		return ret;
	}

	private OLSyntaxNode parseBasicStatement()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;

		switch( token.type() ) {
		case LSQUARE:
			retVal = parseNDChoiceStatement();
			break;
		case PROVIDE:
			getToken();
			retVal = parseProvideUntilStatement();
			break;
		case ID:
			checkConstant();
			String id = token.content();
			getToken();
			
			if ( token.is( Scanner.TokenType.LSQUARE ) || token.is( Scanner.TokenType.DOT ) || token.is( Scanner.TokenType.ASSIGN ) || token.is( Scanner.TokenType.ADD_ASSIGN ) || token.is( Scanner.TokenType.MINUS_ASSIGN ) || token.is( Scanner.TokenType.MULTIPLY_ASSIGN ) || token.is( Scanner.TokenType.DIVIDE_ASSIGN ) || token.is( Scanner.TokenType.POINTS_TO ) || token.is( Scanner.TokenType.DEEP_COPY_LEFT ) || token.is( Scanner.TokenType.DECREMENT ) || token.is( Scanner.TokenType.INCREMENT ) ) {
				retVal = parseAssignOrDeepCopyOrPointerStatement( _parseVariablePath( id ) );
			} else if ( id.equals( "forward" ) && ( token.is( Scanner.TokenType.ID ) || token.is( Scanner.TokenType.LPAREN ) ) ) {
				retVal = parseForwardStatement();
			} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
				retVal = parseInputOperationStatement( id );
			} else if ( token.is( Scanner.TokenType.AT ) ) {
				getToken();
				retVal = parseOutputOperationStatement( id );
			} else {
				retVal = new DefinitionCallStatement( getContext(), id );
			}
			break;
		case WITH:
			getToken();
			retVal = parseInVariablePathProcess( true );
			break;
		case INCREMENT:
			getToken();
			retVal = new PreIncrementStatement( getContext(), parseVariablePath() );
			break;
		case DECREMENT:
			getToken();
			retVal = new PreDecrementStatement( getContext(), parseVariablePath() );
			break;
		case UNDEF:
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();
			retVal =
				new UndefStatement( getContext(), parseVariablePath() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			break;
		case SYNCHRONIZED:
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			assertToken( Scanner.TokenType.ID, "expected lock id" );
			final String sid = token.content();
			getToken();

			eat( Scanner.TokenType.RPAREN, "expected )" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			retVal = new SynchronizedStatement( getContext(), sid, parseProcess() );
			eat( Scanner.TokenType.RCURLY, "expected }" );
			break;
		case SPAWN:
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			VariablePathNode indexVariablePath = parseVariablePath();
			assertToken( Scanner.TokenType.ID, "expected over" );
			if ( token.isKeyword( "over" ) == false ) {
				throwException( "expected over" );
			}
			getToken();
			OLSyntaxNode upperBoundExpression = parseBasicExpression();
			eat( Scanner.TokenType.RPAREN, "expected )" );

			VariablePathNode inVariablePath = null;
			if ( token.isKeyword( "in" ) ) {
				getToken();
				inVariablePath = parseVariablePath();
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
			break;
		case FOR:
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			
			startBackup();
			VariablePathNode leftPath = null;
			
			try {
				leftPath = parseVariablePath();
			} catch( ParserException e ) {}

			if ( leftPath != null && token.isKeyword( "in" ) ) {
				// for( elem in path ) { ... }
				discardBackup();

				getToken();
				VariablePathNode targetPath = parseVariablePath();
				if ( targetPath.path().get( targetPath.path().size() - 1 ).value() != null ) {
					System.out.println( targetPath.path().get( targetPath.path().size() - 1 ).value() );
					throwException( "target in for ( elem -> array ) { ... } should be an array (cannot specify an index): " + targetPath.toPrettyString() );
				}
				eat( Scanner.TokenType.RPAREN, "expected )" );
				final OLSyntaxNode forEachBody = parseBasicStatement();

				retVal = new ForEachArrayItemStatement( getContext(), leftPath, targetPath, forEachBody );
			} else {
				// for( init, condition, post ) { ... }
				recoverBackup();
				OLSyntaxNode init = parseProcess();
				eat( Scanner.TokenType.COMMA, "expected ," );
				OLSyntaxNode condition = parseExpression();
				eat( Scanner.TokenType.COMMA, "expected ," );
				OLSyntaxNode post = parseProcess();
				eat( Scanner.TokenType.RPAREN, "expected )" );

				OLSyntaxNode body = parseBasicStatement();

				retVal = new ForStatement( getContext(), init, condition, post, body );
			}
			break;
		case FOREACH:
			// foreach( k : path ) { ... }
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );

			final VariablePathNode keyPath = parseVariablePath();
			
			eat( Scanner.TokenType.COLON, "expected :" );

			final VariablePathNode targetPath = parseVariablePath();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			final OLSyntaxNode forEachBody = parseBasicStatement();
			retVal = new ForEachSubNodeStatement( getContext(), keyPath, targetPath, forEachBody );
			break;
		case LINKIN:
			retVal = parseLinkInStatement();
			break;
		case CURRENT_HANDLER:
			getToken();
			retVal =
				new CurrentHandlerStatement( getContext() );
			break;
		case NULL_PROCESS:
			getToken();
			retVal =
				new NullProcessStatement( getContext() );
			break;
		case EXIT:
			getToken();
			retVal =
				new ExitStatement( getContext() );
			break;
		case WHILE:
			retVal = parseWhileStatement();
			break;
		case LINKOUT:
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
			break;
		case LPAREN:
			getToken();
			retVal =
				parseProcess();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			break;
		case LCURLY:
			getToken();
			retVal =
				parseProcess();
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
			break;
		case SCOPE:
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected scope identifier" );
			final String scopeId = token.content();
			getToken();

			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			eat(
				Scanner.TokenType.LCURLY, "expected {" );
			retVal =
				new Scope( getContext(), scopeId, parseProcess() );
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
			break;
		case COMPENSATE:
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
			break;
		case THROW:
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
			break;
		case INSTALL:
			getToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			retVal =
				new InstallStatement( getContext(), parseInstallFunction() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			break;
		case IF:
			IfStatement stm = new IfStatement( getContext() );
			OLSyntaxNode cond;

			OLSyntaxNode node;

			getToken();

			eat( Scanner.TokenType.LPAREN, "expected (" );
			cond = parseExpression();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			node = parseBasicStatement();
			stm.addChild( new Pair<>( cond, node ) );

			boolean keepRun = true;
			while ( token.is( Scanner.TokenType.ELSE ) && keepRun ) {
				getToken();
				if ( token.is( Scanner.TokenType.IF ) ) { // else if branch
					getToken();
					eat(
						Scanner.TokenType.LPAREN, "expected (" );
					cond =
						parseExpression();
					eat(
						Scanner.TokenType.RPAREN, "expected )" );
					node =
						parseBasicStatement();
					stm.addChild(
						new Pair<>( cond, node ) );
				} else { // else branch
					keepRun = false;
					stm.setElseProcess( parseBasicStatement() );
				}

			}

			retVal = stm;
			break;
		case DOT:
			if ( inVariablePaths.size() > 0 ) {
				retVal = parseAssignOrDeepCopyOrPointerStatement( parsePrefixedVariablePath() );
			}
			break;
		}

		if ( retVal == null ) {
			throwException( "expected basic statement" );
		}

		return retVal;
	}
	
	private OLSyntaxNode parseProvideUntilStatement()
		throws IOException, ParserException
	{
		ParsingContext context = getContext();
		NDChoiceStatement provide = parseNDChoiceStatement();
		if ( !token.isKeyword( "until" ) ) {
			throwException( "expected until" );
		}
		getToken();
		
		NDChoiceStatement until = parseNDChoiceStatement();
		
		return new ProvideUntilStatement( context, provide, until );
	}
	
	private OLSyntaxNode parseForwardStatement()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal;
		String outputPortName = null;
		if ( token.is( Scanner.TokenType.ID ) ) {
			outputPortName = token.content();
			getToken();
		}
		VariablePathNode outputVariablePath = parseOperationVariablePathParameter();
		if ( token.is( Scanner.TokenType.LPAREN ) ) { // Solicit-Response
			VariablePathNode inputVariablePath = parseOperationVariablePathParameter();
			retVal = new SolicitResponseForwardStatement( getContext(), outputPortName, outputVariablePath, inputVariablePath );
		} else { // Notification
			retVal = new NotificationForwardStatement( getContext(), outputPortName, outputVariablePath );
		}
		
		return retVal;
	}

	private InstallFunctionNode parseInstallFunction()
		throws IOException, ParserException
	{
		boolean backup = insideInstallFunction;
		insideInstallFunction = true;
		List< Pair< String, OLSyntaxNode > > vec = new LinkedList<>();

		boolean keepRun = true;
		List< String > names = new ArrayList<>();
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
				vec.add( new Pair<>( name, handler ) );
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
		} else if ( token.is( Scanner.TokenType.ADD_ASSIGN ) ) {
			getToken();
			retVal =
				new AddAssignStatement( getContext(), path, parseExpression() );
		} else if ( token.is( Scanner.TokenType.MINUS_ASSIGN ) ) {
			getToken();
			retVal =
				new SubtractAssignStatement( getContext(), path, parseExpression() );
		} else if ( token.is( Scanner.TokenType.MULTIPLY_ASSIGN ) ) {
			getToken();
			retVal =
				new MultiplyAssignStatement( getContext(), path, parseExpression() );
		} else if ( token.is( Scanner.TokenType.DIVIDE_ASSIGN ) ) {
			getToken();
			retVal =
				new DivideAssignStatement( getContext(), path, parseExpression() );
		} else if ( token.is( Scanner.TokenType.INCREMENT ) ) {
			getToken();
			retVal =
				new PostIncrementStatement( getContext(), path );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) {
			getToken();
			retVal =
				new PostDecrementStatement( getContext(), path );
		} else if ( token.is( Scanner.TokenType.POINTS_TO ) ) {
			getToken();
			retVal =
				new PointerStatement( getContext(), path, parseVariablePath() );
		} else if ( token.is( Scanner.TokenType.DEEP_COPY_LEFT ) ) {
			getToken();
			retVal = new DeepCopyStatement( getContext(), path, parseExpression() );
		} else {
			throwException( "expected = or -> or << or -- or ++" );
		}

		return retVal;
	}

	private VariablePathNode parseVariablePath()
		throws ParserException, IOException
	{
		if ( token.is( Scanner.TokenType.DOT ) ) {
			return parsePrefixedVariablePath();
		}
		assertToken( Scanner.TokenType.ID, "Expected variable path" );
		String varId = token.content();
		getToken();
		return _parseVariablePath( varId );
	}

	private VariablePathNode _parseVariablePath( String varId )
		throws IOException, ParserException
	{
		OLSyntaxNode expr;
		VariablePathNode path;

		switch( varId ) {
		case Constants.GLOBAL:
			path = new VariablePathNode( getContext(), Type.GLOBAL );
			break;
		case Constants.CSETS:
			path = new VariablePathNode( getContext(), Type.CSET );
			path.append( new Pair<>( new ConstantStringExpression( getContext(), varId ), new ConstantIntegerExpression( getContext(), 0 ) ) );
			break;
		default:
			path = new VariablePathNode( getContext(), Type.NORMAL );
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				getToken();
				expr = parseBasicExpression();
				eat(
					Scanner.TokenType.RSQUARE, "expected ]" );
			} else {
				expr = null;
			}
			path.append( new Pair<>( new ConstantStringExpression( getContext(), varId ), expr ) );
			break;
		}

		OLSyntaxNode nodeExpr = null;
		while ( token.is( Scanner.TokenType.DOT ) ) {
			getToken();
			if ( token.isIdentifier() ) {
				nodeExpr = new ConstantStringExpression( getContext(), token.content() );
			} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
				getToken();
				nodeExpr = parseBasicExpression();
				assertToken(
					Scanner.TokenType.RPAREN, "expected )" );
			} else {
				throwException( "expected nested node identifier" );
			}

			getToken();
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				getToken();
				expr = parseBasicExpression();
				eat( Scanner.TokenType.RSQUARE, "expected ]" );
			} else {
				expr = null;
			}

			path.append( new Pair<>( nodeExpr, expr ) );
		}

		return path;
	}

	private VariablePathNode parsePrefixedVariablePath()
		throws IOException, ParserException
	{
		int i = inVariablePaths.size() - 1;
		List< Scanner.Token > tokens = new ArrayList<>();
		try {
			tokens.addAll( inVariablePaths.get( i ) );
		} catch( IndexOutOfBoundsException e ) {
			throwException( "Prefixed variable paths must be inside a with block" );
		}

		while ( tokens.get( 0 ).is( Scanner.TokenType.DOT ) ) {
			i--;
			tokens.addAll( 0, inVariablePaths.get( i ) );
		}

		addTokens( tokens );
		addTokens( Arrays.asList( new Scanner.Token( Scanner.TokenType.DOT ) ) );
		getToken();

		String varId = token.content();
		getToken();

		return _parseVariablePath( varId );
	}
	
	private CourierChoiceStatement parseCourierChoice()
		throws IOException, ParserException
	{
		CourierChoiceStatement stm = new CourierChoiceStatement( getContext() );
		OLSyntaxNode body;
		InterfaceDefinition iface;
		String operationName;
		VariablePathNode inputVariablePath, outputVariablePath;

		while ( token.is( Scanner.TokenType.LSQUARE ) ) {
			iface = null; operationName = null;
			inputVariablePath = null; outputVariablePath = null;
			getToken();
			if ( token.isKeyword( "interface" ) ) {
				getToken();
				assertToken( Scanner.TokenType.ID, "expected interface name" );
				checkConstant();
				iface = interfaces.get( token.content() );
				if ( iface == null ) {
					throwException( "undefined interface: " + token.content() );
				}
				getToken();
				inputVariablePath = parseOperationVariablePathParameter();
				if ( inputVariablePath == null ) {
					throwException( "expected variable path" );
				}
				if ( token.is( Scanner.TokenType.LPAREN ) ) { // Request-Response
					outputVariablePath = parseOperationVariablePathParameter();
				}
			} else if ( token.is( Scanner.TokenType.ID ) ) {
				operationName = token.content();
				getToken();
				inputVariablePath = parseOperationVariablePathParameter();
				if ( inputVariablePath == null ) {
					throwException( "expected variable path" );
				}
				if ( token.is( Scanner.TokenType.LPAREN ) ) { // Request-Response
					outputVariablePath = parseOperationVariablePathParameter();
				}
			} else {
				throwException( "expected courier input guard (interface or operation name)" );
			}

			eat( Scanner.TokenType.RSQUARE, "expected ]" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			body = parseProcess();
			eat( Scanner.TokenType.RCURLY, "expected }" );

			if ( iface == null ) { // It's an operation
				if ( outputVariablePath == null ) { // One-Way
					stm.operationOneWayBranches().add( new CourierChoiceStatement.OperationOneWayBranch( operationName, inputVariablePath, body ) );
				} else { // Request-Response
					stm.operationRequestResponseBranches().add( new CourierChoiceStatement.OperationRequestResponseBranch( operationName, inputVariablePath, outputVariablePath, body ) );
				}
			} else { // It's an interface
				if ( outputVariablePath == null ) { // One-Way
					stm.interfaceOneWayBranches().add( new CourierChoiceStatement.InterfaceOneWayBranch( iface, inputVariablePath, body ) );
				} else { // Request-Response
					stm.interfaceRequestResponseBranches().add( new CourierChoiceStatement.InterfaceRequestResponseBranch( iface, inputVariablePath, outputVariablePath, body ) );
				}
			}
		}

		return stm;
	}

	private NDChoiceStatement parseNDChoiceStatement()
		throws IOException, ParserException
	{
		NDChoiceStatement stm = new NDChoiceStatement( getContext() );
		OLSyntaxNode inputGuard = null;
		OLSyntaxNode process;

		while ( token.is( Scanner.TokenType.LSQUARE ) ) {
			getToken(); // Eat [
			/*if ( token.is( Scanner.TokenType.LINKIN ) ) {
				inputGuard = parseLinkInStatement();
			} else */if ( token.is( Scanner.TokenType.ID ) ) {
				String id = token.content();
				getToken();
				inputGuard = parseInputOperationStatement( id );
			} else {
				throwException( "expected input guard" );
			}

			eat( Scanner.TokenType.RSQUARE, "expected ]" );
			if ( token.is( Scanner.TokenType.LCURLY ) ) {
				eat( Scanner.TokenType.LCURLY, "expected {" );
				process = parseProcess();
				eat( Scanner.TokenType.RCURLY, "expected }" );
			} else {
				process = new NullProcessStatement( getContext() );
			}
			
			stm.addChild( new Pair<>( inputGuard, process ) );
		}

		return stm;
	}

	private LinkInStatement parseLinkInStatement()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		assertToken( Scanner.TokenType.ID, "expected link identifier" );
		LinkInStatement stm = new LinkInStatement( getContext(), token.content() );
		getToken();

		eat( Scanner.TokenType.RPAREN, "expected )" );
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
			OLSyntaxNode process = new NullProcessStatement( getContext() );
			if ( token.is( Scanner.TokenType.LCURLY ) ) { // Request Response body
				getToken();
				process = parseProcess();
				eat( Scanner.TokenType.RCURLY, "expected }" );
			}
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

		eat( Scanner.TokenType.LPAREN, "expected (" );
		if ( token.is( Scanner.TokenType.ID ) ) {
			ret = parseVariablePath();
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

		eat( Scanner.TokenType.LPAREN, "expected (" );
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
				context,
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

		eat( Scanner.TokenType.LPAREN, "expected (" );
		cond = parseExpression();
		eat( Scanner.TokenType.RPAREN, "expected )" );
		eat( Scanner.TokenType.LCURLY, "expected {" );
		process =
			parseProcess();
		eat( Scanner.TokenType.RCURLY, "expected }" );
		return new WhileStatement( context, cond, process );
	}

	private OLSyntaxNode parseExpression()
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

	private OLSyntaxNode parseBasicCondition()
		throws IOException, ParserException
	{
		OLSyntaxNode ret;

		Scanner.TokenType opType;
		OLSyntaxNode expr1;

		expr1 =	parseBasicExpression();

		opType = token.type();
		if ( opType == Scanner.TokenType.EQUAL || opType == Scanner.TokenType.LANGLE ||
			opType == Scanner.TokenType.RANGLE || opType == Scanner.TokenType.MAJOR_OR_EQUAL ||
			opType == Scanner.TokenType.MINOR_OR_EQUAL || opType == Scanner.TokenType.NOT_EQUAL
		) {
			OLSyntaxNode expr2;
			getToken();
			expr2 = parseBasicExpression();
			ret = new CompareConditionNode( getContext(), expr1, expr2, opType );
		} else if ( opType == Scanner.TokenType.INSTANCE_OF ) {
			getToken();
			TypeDefinition type;
			NativeType nativeType = readNativeType();
			if ( nativeType == null ) { // It's a user-defined type
				assertToken( Scanner.TokenType.ID, "expected type name after instanceof" );
			}
			if ( definedTypes.containsKey( token.content() ) == false ) {
				throwException( "invalid type: " + token.content() );
			}
			type = definedTypes.get( token.content() );
			ret = new InstanceOfExpressionNode( getContext(), expr1, type );
			getToken();
		} else {
			ret = expr1;
		}

		if ( ret == null ) {
			throwException( "expected condition" );
		}

		return ret;
	}

	/*
	 * todo: Check if negative integer handling is appropriate
	 */
	private OLSyntaxNode parseBasicExpression()
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
			} else if ( token.is( Scanner.TokenType.LONG ) ) { // e.g. i -1L
				long value = Long.parseLong( token.content() );
				// We add it, because it's already negative.
				if ( value < 0 ) {
					sum.add( parseProductExpression() );
				} else { // e.g. i 1
					throwException( "expected expression operator" );
				}
			} else if ( token.is( Scanner.TokenType.DOUBLE ) ) { // e.g. i -1
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

		switch( token.type() ) {
		case ID:
			path = parseVariablePath();
			VariablePathNode freshValuePath = new VariablePathNode( getContext(), Type.NORMAL );
			freshValuePath.append( new Pair<>( new ConstantStringExpression( getContext(), "new" ), new ConstantIntegerExpression( getContext(), 0 ) ) );
			if ( path.isEquivalentTo( freshValuePath ) ) {
				retVal = new FreshValueExpressionNode( path.context() );
				return retVal;
			}
			break;
		case DOT:
			path = parseVariablePath();
			break;
		case CARET:
			if ( insideInstallFunction ) {
				getToken();
				path = parseVariablePath();
				retVal = new InstallFixedVariableExpressionNode( getContext(), path );
				return retVal;
			}
			break;
		}

		if ( path != null ) {
			switch( token.type() ) {
			case INCREMENT:
				getToken();
				retVal =
					new PostIncrementStatement( getContext(), path );
				break;
			case DECREMENT:
				getToken();
				retVal =
					new PostDecrementStatement( getContext(), path );
				break;
			case ASSIGN:
				getToken();
				retVal = new AssignStatement( getContext(), path, parseExpression() );
				break;
			default:
				retVal = new VariableExpressionNode( getContext(), path );
				break;
			}
		} else {
			switch( token.type() ) {
			case NOT:
				getToken();
				retVal = new NotExpressionNode( getContext(), parseFactor() );
				break;
			case STRING:
				retVal = new ConstantStringExpression( getContext(), token.content() );
				getToken();
				break;
			case INT:
				retVal = new ConstantIntegerExpression( getContext(), Integer.parseInt( token.content() ) );
				getToken();
				break;
			case LONG:
				retVal = new ConstantLongExpression( getContext(), Long.parseLong( token.content() ) );
				getToken();
				break;
			case TRUE:
				retVal = new ConstantBoolExpression( getContext(), true );
				getToken();
				break;
			case FALSE:
				retVal = new ConstantBoolExpression( getContext(), false );
				getToken();
				break;
			case DOUBLE:
				retVal = new ConstantDoubleExpression( getContext(), Double.parseDouble( token.content() ) );
				getToken();
				break;
			case LPAREN:
				getToken();
				retVal = parseExpression();
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case HASH:
				getToken();
				retVal = new ValueVectorSizeExpressionNode(
					getContext(),
					parseVariablePath()
				);
				break;
			case INCREMENT:
				getToken();
				retVal = new PreIncrementStatement( getContext(), parseVariablePath() );
				break;
			case DECREMENT:
				getToken();
				retVal = new PreDecrementStatement( getContext(), parseVariablePath() );
				break;
			case IS_DEFINED:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.DEFINED,
					parseVariablePath()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_INT:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.INT,
					parseVariablePath()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_DOUBLE:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.DOUBLE,
					parseVariablePath()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_BOOL:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.BOOL,
					parseVariablePath()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_LONG:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.LONG,
					parseVariablePath()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_STRING:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.STRING,
					parseVariablePath()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_INT:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode( getContext(), NativeType.INT, parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_LONG:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode( getContext(), NativeType.LONG, parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_BOOL:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode( getContext(), NativeType.BOOL, parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_DOUBLE:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode(
					getContext(),
					NativeType.DOUBLE,
					parseExpression()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_STRING:
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode(
					getContext(),
					NativeType.STRING,
					parseExpression()
				);
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			}
		}	
				
		if ( retVal == null ) {
			if ( token.is( Scanner.TokenType.LCURLY ) ) {
				retVal = new VoidExpressionNode( getContext() );
			} else {
				throwException( "expected expression" );
			}
		}
		
		if ( token.is( Scanner.TokenType.LCURLY ) ) {
			retVal = parseInlineTreeExpression( retVal );
		}

		return retVal;
	}
	
	private OLSyntaxNode parseInlineTreeExpression( OLSyntaxNode rootExpression )
		throws IOException, ParserException
	{
		eat( Scanner.TokenType.LCURLY, "expected {" );
		
		boolean keepRun = true;
		VariablePathNode path;
		OLSyntaxNode expression;
		
		List< Pair< VariablePathNode, OLSyntaxNode > > assignments = new ArrayList<>();
		
		while( keepRun ) {
			eat( Scanner.TokenType.DOT, "expected ." );
			
			path = parseVariablePath();
			eat( Scanner.TokenType.ASSIGN, "expected =" );
			expression = parseExpression();
			
			assignments.add( new Pair<>( path, expression ) );
			
			if ( token.is( Scanner.TokenType.COMMA ) ) {
				getToken();
			} else {
				keepRun = false;
			}
		}
		
		eat( Scanner.TokenType.RCURLY, "expected }" );
		
		return new InlineTreeExpressionNode( rootExpression.context(), rootExpression, assignments.toArray( new Pair[0] ) );
	}

	private OLSyntaxNode parseProductExpression()
		throws IOException, ParserException
	{
		ProductExpressionNode product = new ProductExpressionNode( getContext() );
		product.multiply( parseFactor() );
		boolean keepRun = true;
		while ( keepRun ) {
			switch( token.type() ) {
			case ASTERISK:
				getToken();
				product.multiply( parseFactor() );
				break;
			case DIVIDE:
				getToken();
				product.divide( parseFactor() );
				break;
			case PERCENT_SIGN:
				getToken();
				product.modulo( parseFactor() );
				break;
			default:
				keepRun = false;
				break;
			}
		}

		return product;
	}
	
	private static class IncludeFile {
		private final InputStream inputStream;
		private final String parentPath;
		private final URI uri;
		private IncludeFile( InputStream inputStream, String parentPath, URI uri )
		{
			this.inputStream = inputStream;
			this.parentPath = parentPath;
			this.uri = uri;
		}

		private InputStream getInputStream()
		{
			return inputStream;
		}

		private String getParentPath()
		{
			return parentPath;
		}

		private URI getURI()
		{
			return uri;
		}
	}
}
