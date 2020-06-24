/*
 * Copyright (C) 2006-2020 Fabrizio Montesi <famontesi@gmail.com>
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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import jolie.lang.parse.util.ProgramBuilder;
import jolie.util.Helpers;
import jolie.util.Pair;
import jolie.util.Range;
import jolie.util.UriUtils;

/**
 * Parser for a .ol file.
 * 
 * @author Fabrizio Montesi
 *
 */
public class OLParser extends AbstractParser {
	private interface ParsingRunnable {
		public void parse() throws IOException, ParserException;
	}

	private Optional< String > serviceName = Optional.empty();
	private final ProgramBuilder programBuilder;
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

	public OLParser( Scanner scanner, String[] includePaths, ClassLoader classLoader ) {
		super( scanner );
		final ParsingContext context = new URIParsingContext( scanner.source(), 0 );
		this.programBuilder = new ProgramBuilder( context );
		this.includePaths = includePaths;
		this.classLoader = classLoader;
		this.definedTypes = createTypeDeclarationMap( context );
	}

	public void putConstants( Map< String, Scanner.Token > constantsToPut ) {
		constantsMap.putAll( constantsToPut );
	}

	public static Map< String, TypeDefinition > createTypeDeclarationMap( ParsingContext context ) {
		Map< String, TypeDefinition > definedTypes = new HashMap<>();

		// Fill in defineTypes with all the supported native types (string, int, double, ...)
		for( NativeType type : NativeType.values() ) {
			definedTypes.put( type.id(),
				new TypeInlineDefinition( context, type.id(), type, Constants.RANGE_ONE_TO_ONE ) );
		}
		definedTypes.put( TypeDefinitionUndefined.UNDEFINED_KEYWORD, TypeDefinitionUndefined.getInstance() );

		return definedTypes;
	}

	public Program parse()
		throws IOException, ParserException {
		nextToken();
		boolean explicitServiceBlock = token.isKeyword( "service" );
		if( token.isKeyword( "service" ) ) { // Top-level service definition
			nextToken();
			assertToken( Scanner.TokenType.ID, "expected service name" );
			serviceName = Optional.of( token.content() );
			nextToken();
			assertToken( Scanner.TokenType.LCURLY,
				"expected { after the opening clause of service " + serviceName.get() );
		} else {
			addToken( token );
		}

		_parse( explicitServiceBlock );

		if( initSequence != null ) {
			programBuilder.addChild( new DefinitionNode( getContext(), "init", initSequence ) );
		}

		if( main != null ) {
			programBuilder.addChild( main );
		}

		return programBuilder.toProgram();
	}

	private void parseLoop( boolean explicitServiceBlock, ParsingRunnable... parseRunnables )
		throws IOException, ParserException {
		nextToken();
		if( token.is( Scanner.TokenType.HASH ) ) {
			// Shebang scripting
			scanner().readLine();
			nextToken();
		}

		Scanner.Token t;
		do {
			t = token;
			for( ParsingRunnable runnable : parseRunnables ) {
				parseInclude();
				runnable.parse();
			}
		} while( t != token ); // Loop until no procedures can eat the initial token

		if( explicitServiceBlock ) {
			eat( Scanner.TokenType.RCURLY, "expected } at the end of the definition of service " + serviceName.get() );
			t = token;
		}

		if( t.isNot( Scanner.TokenType.EOF ) ) {
			throwException( "Invalid token encountered" );
		}
	}

	private void _parse( boolean explicitServiceBlock )
		throws IOException, ParserException {
		parseLoop( explicitServiceBlock,
			this::parseConstants,
			this::parseExecution,
			this::parseCorrelationSets,
			this::parseTypes,
			this::parseInterfaceOrPort,
			this::parseEmbedded,
			this::parseInternalService,
			this::parseCode );
	}

	private void parseTypes()
		throws IOException, ParserException {
		Scanner.Token commentToken = null;
		boolean keepRun = true;
		boolean haveComment = false;
		while( keepRun ) {
			if( token.is( Scanner.TokenType.DOCUMENTATION_FORWARD ) ) {
				haveComment = true;
				commentToken = token;
				nextToken();
			} else if( token.isKeyword( "type" ) ) {
				String typeName;
				TypeDefinition currentType;

				nextToken();
				typeName = token.content();
				eat( Scanner.TokenType.ID, "expected type name" );
				if( token.is( Scanner.TokenType.COLON ) ) {
					nextToken();
				} else {
					prependToken( new Scanner.Token( Scanner.TokenType.ID, NativeType.VOID.id() ) );
					nextToken();
				}

				currentType = parseType( typeName );

				if( haveComment ) {
					haveComment = false;
				}
				parseBackwardAndSetDocumentation( currentType, Optional.ofNullable( commentToken ) );
				commentToken = null;

				typeName = currentType.id();

				definedTypes.put( typeName, currentType );
				programBuilder.addChild( currentType );
			} else {
				keepRun = false;
				if( haveComment ) { // we return the comment and the subsequent token since we did not use them
					addToken( commentToken );
					addToken( token );
					nextToken();
				}
			}
		}
	}

	private TypeDefinition parseType( String typeName )
		throws IOException, ParserException {
		TypeDefinition currentType;

		NativeType nativeType = readNativeType();
		if( nativeType == null ) { // It's a user-defined type
			currentType = new TypeDefinitionLink( getContext(), typeName, Constants.RANGE_ONE_TO_ONE, token.content() );
			nextToken();
		} else {
			currentType = new TypeInlineDefinition( getContext(), typeName, nativeType, Constants.RANGE_ONE_TO_ONE );
			nextToken();
			if( token.is( Scanner.TokenType.LCURLY ) ) { // We have sub-types to parse
				parseSubTypes( (TypeInlineDefinition) currentType );
			}
		}

		if( token.is( Scanner.TokenType.PARALLEL ) ) { // It's a sum (union, choice) type
			nextToken();
			final TypeDefinition secondType = parseType( typeName );
			return new TypeChoiceDefinition( getContext(), typeName, Constants.RANGE_ONE_TO_ONE, currentType,
				secondType );
		}

		return currentType;
	}

	private void parseSubTypes( TypeInlineDefinition type )
		throws IOException, ParserException {
		eat( Scanner.TokenType.LCURLY, "expected {" );

		Optional< Scanner.Token > commentToken = Optional.empty();
		boolean keepRun = true;
		while( keepRun ) {
			if( token.is( Scanner.TokenType.DOCUMENTATION_FORWARD ) ) {
				commentToken = Optional.of( token );
				nextToken();
			} else if( token.is( Scanner.TokenType.QUESTION_MARK ) ) {
				type.setUntypedSubTypes( true );
				nextToken();
			} else {
				TypeDefinition currentSubType;
				while( !token.is( Scanner.TokenType.RCURLY ) ) {
					if( token.is( Scanner.TokenType.DOCUMENTATION_FORWARD ) ) {
						commentToken = Optional.of( token );
						nextToken();
					} else {
						if( token.is( Scanner.TokenType.DOT ) ) {
							if( hasMetNewline() ) {
								nextToken();
							} else {
								throwException(
									"the dot prefix operator for type nodes is allowed only after a newline" );
							}
						}

						// SubType id
						String id = token.content();
						if( token.is( Scanner.TokenType.STRING ) ) {
							nextToken();
						} else {
							eatIdentifier( "expected type node name" );
						}

						Range cardinality = parseCardinality();
						if( token.is( Scanner.TokenType.COLON ) ) {
							nextToken();
						} else {
							prependToken( new Scanner.Token( Scanner.TokenType.ID, NativeType.VOID.id() ) );
							nextToken();
						}

						currentSubType = parseSubType( id, cardinality );

						parseBackwardAndSetDocumentation( currentSubType, commentToken );
						commentToken = Optional.empty();

						if( type.hasSubType( currentSubType.id() ) ) {
							throwException( "sub-type " + currentSubType.id()
								+ " conflicts with another sub-type with the same name" );
						}
						type.putSubType( currentSubType );
					}
				}

				keepRun = false;
				// if ( haveComment ) {
				// addToken( commentToken );
				// addToken( token );
				// getToken();
				// }
			}
		}
		eat( Scanner.TokenType.RCURLY, "RCURLY expected" );
	}

	private TypeDefinition parseSubType( String id, Range cardinality )
		throws IOException, ParserException {
		NativeType nativeType = readNativeType();
		TypeDefinition subType;
		// SubType id

		if( nativeType == null ) { // It's a user-defined type
			subType = new TypeDefinitionLink( getContext(), id, cardinality, token.content() );
			nextToken();
		} else {
			nextToken();
			subType = new TypeInlineDefinition( getContext(), id, nativeType, cardinality );

			Optional< Scanner.Token > commentToken = Optional.empty();
			if( token.is( Scanner.TokenType.DOCUMENTATION_BACKWARD ) ) {
				commentToken = Optional.of( token );
				nextToken();
			}
			if( token.is( Scanner.TokenType.LCURLY ) ) { // Has ulterior sub-types
				parseSubTypes( (TypeInlineDefinition) subType );
			}
			if( commentToken.isPresent() ) { // we return the backward comment token and the eaten one, to be parsed by
												// the super type
				addToken( commentToken.get() );
				addToken( new Scanner.Token( Scanner.TokenType.NEWLINE ) );
				addToken( token );
				nextToken();
			}
		}

		if( token.is( Scanner.TokenType.PARALLEL ) ) {
			nextToken();
			TypeDefinition secondSubType = parseSubType( id, cardinality );
			return new TypeChoiceDefinition( getContext(), id, cardinality, subType, secondSubType );
		}
		return subType;
	}

	private NativeType readNativeType() {
		if( token.is( Scanner.TokenType.CAST_INT ) ) {
			return NativeType.INT;
		} else if( token.is( Scanner.TokenType.CAST_DOUBLE ) ) {
			return NativeType.DOUBLE;
		} else if( token.is( Scanner.TokenType.CAST_STRING ) ) {
			return NativeType.STRING;
		} else if( token.is( Scanner.TokenType.CAST_LONG ) ) {
			return NativeType.LONG;
		} else if( token.is( Scanner.TokenType.CAST_BOOL ) ) {
			return NativeType.BOOL;
		} else {
			return NativeType.fromString( token.content() );
		}
	}

	private Range parseCardinality()
		throws IOException, ParserException {
		final int min;
		final int max;

		if( token.is( Scanner.TokenType.QUESTION_MARK ) ) {
			min = 0;
			max = 1;
			nextToken();
		} else if( token.is( Scanner.TokenType.ASTERISK ) ) {
			min = 0;
			max = Integer.MAX_VALUE;
			nextToken();
		} else if( token.is( Scanner.TokenType.LSQUARE ) ) {
			nextToken(); // eat [

			// Minimum
			assertToken( Scanner.TokenType.INT, "expected int value" );
			min = Integer.parseInt( token.content() );
			if( min < 0 ) {
				throwException( "Minimum number of occurrences of a sub-type must be positive or zero" );
			}

			nextToken();
			eat( Scanner.TokenType.COMMA, "expected comma separator" );

			// Maximum
			if( token.is( Scanner.TokenType.INT ) ) {
				max = Integer.parseInt( token.content() );
				if( max < 1 ) {
					throwException( "Maximum number of occurrences of a sub-type must be positive" );
				}
			} else if( token.is( Scanner.TokenType.ASTERISK ) ) {
				max = Integer.MAX_VALUE;
			} else {
				max = -1;
				throwException( "Maximum number of sub-type occurrences not valid: " + token.content() );
			}

			nextToken();
			eat( Scanner.TokenType.RSQUARE, "expected ]" );
		} else { // Default (no cardinality specified)
			min = 1;
			max = 1;
		}

		return new Range( min, max );
	}


	private void parseEmbedded()
		throws IOException, ParserException {
		if( token.isKeyword( "embedded" ) ) {
			String servicePath, portId;
			nextToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			boolean keepRun = true;
			Constants.EmbeddedServiceType type;
			while( keepRun ) {
				type = null;
				if( token.isKeyword( "Java" ) ) {
					type = Constants.EmbeddedServiceType.JAVA;
				} else if( token.isKeyword( "Jolie" ) ) {
					type = Constants.EmbeddedServiceType.JOLIE;
				} else if( token.isKeyword( "JavaScript" ) ) {
					type = Constants.EmbeddedServiceType.JAVASCRIPT;
				}
				if( type == null ) {
					keepRun = false;
				} else {
					nextToken();
					eat( Scanner.TokenType.COLON, "expected : after embedded service type" );
					checkConstant();
					while( token.is( Scanner.TokenType.STRING ) ) {
						servicePath = token.content();
						nextToken();
						if( token.isKeyword( "in" ) ) {
							eatKeyword( "in", "expected in" );
							assertToken( Scanner.TokenType.ID, "expected output port name" );
							portId = token.content();
							nextToken();
						} else {
							portId = null;
						}
						programBuilder.addChild(
							new EmbeddedServiceNode(
								getContext(),
								type,
								servicePath,
								portId ) );
						if( token.is( Scanner.TokenType.COMMA ) ) {
							nextToken();
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
		throws IOException, ParserException {
		while( token.isKeyword( "cset" ) ) {
			nextToken();
			/*
			 * assertToken( Scanner.TokenType.ID, "expected correlation set name" ); String csetName =
			 * token.content(); getToken();
			 */
			eat( Scanner.TokenType.LCURLY, "expected {" );
			List< CorrelationVariableInfo > variables = new LinkedList<>();
			List< CorrelationAliasInfo > aliases;
			VariablePathNode correlationVariablePath;
			String typeName;
			while( token.is( Scanner.TokenType.ID ) ) {
				aliases = new LinkedList<>();
				correlationVariablePath = parseVariablePath();
				eat( Scanner.TokenType.COLON, "expected correlation variable alias list" );
				assertToken( Scanner.TokenType.ID, "expected correlation variable alias" );
				while( token.is( Scanner.TokenType.ID ) ) {
					typeName = token.content();
					nextToken();
					eat( Scanner.TokenType.DOT, "expected . after message type name in correlation alias" );
					aliases.add( new CorrelationAliasInfo( typeName, parseVariablePath() ) );
				}
				variables.add( new CorrelationVariableInfo( correlationVariablePath, aliases ) );
				if( token.is( Scanner.TokenType.COMMA ) ) {
					nextToken();
				} else {
					break;
				}
			}

			programBuilder.addChild( new CorrelationSetInfo( getContext(), variables ) );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}

	private void parseExecution()
		throws IOException, ParserException {
		if( token.is( Scanner.TokenType.EXECUTION ) ) {
			Constants.ExecutionMode mode = Constants.ExecutionMode.SEQUENTIAL;
			nextToken();
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
			programBuilder.addChild( new ExecutionInfo( getContext(), mode ) );
			nextToken();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}

	private void parseConstants()
		throws IOException, ParserException {
		if( token.is( Scanner.TokenType.CONSTANTS ) ) {
			nextToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			boolean keepRun = true;
			while( token.is( Scanner.TokenType.ID ) && keepRun ) {
				String cId = token.content();
				nextToken();
				eat( Scanner.TokenType.ASSIGN, "expected =" );
				if( token.isValidConstant() == false ) {
					throwException( "expected string, integer, double or identifier constant" );
				}
				if( constantsMap.containsKey( cId ) == false ) {
					constantsMap.put( cId, token );
				}
				nextToken();
				if( token.isNot( Scanner.TokenType.COMMA ) ) {
					keepRun = false;
				} else {
					nextToken();
				}
			}
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}

	private URL guessIncludeFilepath( String urlStr, String filename, String path ) {
		try {
			if( urlStr.startsWith( "jap:" ) || urlStr.startsWith( "jar:" ) ) {
				// Try hard to resolve names, even in Windows
				if( filename.startsWith( "../" ) ) {
					String tmpPath = path;
					String tmpFilename = filename;
					if( !tmpPath.contains( "/" ) && tmpPath.contains( "\\" ) ) { // Windows only
						tmpPath = tmpPath.replace( "\\", "/" );
					}
					while( tmpFilename.startsWith( "../" ) ) {
						tmpFilename = tmpFilename.substring( 2 );
						if( tmpPath.endsWith( "/" ) ) {
							tmpPath = tmpPath.substring( 0, tmpPath.length() - 1 );
						}
						tmpPath = tmpPath.substring( 0, tmpPath.lastIndexOf( "/" ) );
					}

					String tmpUrl = build( tmpPath, tmpFilename );
					try {
						return new URL( tmpUrl.substring( 0, 4 ) + tmpUrl.substring( 4 ) );
					} catch( Exception exn ) {
						return null;
					}
				} else if( filename.startsWith( "./" ) ) {
					String tmpPath = path;
					String tmpFilename = filename;
					if( !tmpPath.contains( "/" ) && tmpPath.contains( "\\" ) ) {
						tmpPath = tmpPath.replace( "\\", "/" );
					}
					tmpFilename = tmpFilename.substring( 1 );
					if( tmpPath.endsWith( "/" ) ) {
						tmpPath = tmpPath.substring( 0, tmpPath.length() - 1 );
					}
					String tmpUrl = build( tmpPath, tmpFilename );
					return new URL( tmpUrl.substring( 0, 4 ) + tmpUrl.substring( 4 ) );
				} else {
					/*
					 * We need the embedded URL path, otherwise URI.normalize is going to do nothing.
					 */
					return new URL(
						urlStr.substring( 0, 4 ) + new URI( urlStr.substring( 4 ) ).normalize().toString() );
				}
			} else {
				return new URL( new URI( urlStr ).normalize().toString() );
			}
		} catch( MalformedURLException | URISyntaxException e ) {
			return null;
		}
	}

	private IncludeFile retrieveIncludeFile( final String context, final String target )
		throws URISyntaxException {
		IncludeFile ret;

		String urlStr =
			UriUtils.normalizeJolieUri( UriUtils.normalizeWindowsPath( UriUtils.resolve( context, target ) ) );
		ret = tryAccessIncludeFile( urlStr );

		if( ret == null ) {
			final URL url = guessIncludeFilepath( urlStr, target, context );
			if( url != null ) {
				ret = tryAccessIncludeFile( url.toString() );
			}
		}

		return ret;
	}

	private final Map< String, URL > resourceCache = new HashMap<>();

	private IncludeFile tryAccessIncludeFile( String origIncludeStr ) {
		final String includeStr = UriUtils.normalizeWindowsPath( origIncludeStr );

		final Optional< IncludeFile > optIncludeFile = Helpers.firstNonNull(
			() -> {
				final URL url = resourceCache.get( includeStr );
				if( url == null ) {
					return null;
				}
				try {
					return new IncludeFile( new BufferedInputStream( url.openStream() ), Helpers.parentFromURL( url ),
						url.toURI() );
				} catch( IOException | URISyntaxException e ) {
					return null;
				}
			},
			() -> {
				try {
					Path path = Paths.get( includeStr );
					return new IncludeFile( new BufferedInputStream( Files.newInputStream( path ) ),
						path.getParent().toString(), path.toUri() );
				} catch( FileSystemNotFoundException | IOException | InvalidPathException e ) {
					return null;
				}
			},
			() -> {
				try {
					final URL url = new URL( includeStr );
					final InputStream is = url.openStream();
					return new IncludeFile( new BufferedInputStream( is ), Helpers.parentFromURL( url ), url.toURI() );
				} catch( IOException | URISyntaxException e ) {
					return null;
				}
			},
			() -> {
				final URL url = classLoader.getResource( includeStr );
				if( url == null ) {
					return null;
				}

				try {
					return new IncludeFile( new BufferedInputStream( url.openStream() ), Helpers.parentFromURL( url ),
						url.toURI() );
				} catch( IOException | URISyntaxException e ) {
					return null;
				}
			} );

		optIncludeFile.ifPresent( includeFile -> {
			try {
				resourceCache.putIfAbsent( includeStr, includeFile.uri.toURL() );
			} catch( MalformedURLException e ) {
				e.printStackTrace();
			}
		} );

		return optIncludeFile.orElse( null );
	}

	private void parseInclude()
		throws IOException, ParserException {
		String[] origIncludePaths;
		IncludeFile includeFile;
		while( token.is( Scanner.TokenType.INCLUDE ) ) {
			nextToken();
			Scanner oldScanner = scanner();
			assertToken( Scanner.TokenType.STRING, "expected filename to include" );
			String includeStr = token.content();
			includeFile = null;

			for( int i = 0; i < includePaths.length && includeFile == null; i++ ) {
				try {
					includeFile = retrieveIncludeFile( includePaths[ i ], includeStr );
				} catch( URISyntaxException e ) {
					throw new ParserException( getContext(), e.getMessage() );
				}
			}

			if( includeFile == null ) {
				includeFile = tryAccessIncludeFile( includeStr );
				if( includeFile == null ) {
					throwException( "File not found: " + includeStr );
				}
			}

			origIncludePaths = includePaths;
			// includes are explicitly parsed in ASCII to be independent of program's encoding
			setScanner( new Scanner( includeFile.getInputStream(), includeFile.getURI(), "US-ASCII",
				oldScanner.includeDocumentation() ) );

			if( includeFile.getParentPath() == null ) {
				includePaths = Arrays.copyOf( origIncludePaths, origIncludePaths.length );
			} else {
				includePaths = Arrays.copyOf( origIncludePaths, origIncludePaths.length + 1 );
				includePaths[ origIncludePaths.length ] = includeFile.getParentPath();
			}
			_parse( false );
			includePaths = origIncludePaths;
			includeFile.getInputStream().close();
			setScanner( oldScanner );
			nextToken();
		}
	}

	private boolean checkConstant() {
		if( token.is( Scanner.TokenType.ID ) ) {
			final Scanner.Token t;
			final Constants.Predefined p = Constants.Predefined.get( token.content() );
			if( p != null ) {
				t = p.token();
			} else {
				t = constantsMap.get( token.content() );
			}
			if( t != null ) {
				token = t;
				return true;
			}
		}
		return false;
	}

	private PortInfo parsePort()
		throws IOException, ParserException {
		PortInfo portInfo = null;
		if( token.isKeyword( "inputPort" ) ) {
			portInfo = parseInputPortInfo();
		} else if( token.isKeyword( "outputPort" ) ) {
			nextToken();
			assertToken( Scanner.TokenType.ID, "expected output port identifier" );
			OutputPortInfo p = new OutputPortInfo( getContext(), token.content() );
			nextToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			parseOutputPortInfo( p );
			programBuilder.addChild( p );
			eat( Scanner.TokenType.RCURLY, "expected }" );
			portInfo = p;
		}
		return portInfo;
	}

	private Optional< Scanner.Token > parseForwardDocumentation()
		throws IOException {
		Scanner.Token docToken = null;
		while( token.is( Scanner.TokenType.DOCUMENTATION_FORWARD ) ) {
			docToken = token;
			nextToken();
		}
		return Optional.ofNullable( docToken );
	}

	private Optional< Scanner.Token > parseBackwardDocumentation()
		throws IOException {
		Scanner.Token docToken = null;
		while( token.is( Scanner.TokenType.DOCUMENTATION_BACKWARD ) ) {
			docToken = token;
			nextToken();
		}
		return Optional.ofNullable( docToken );
	}

	private void parseBackwardAndSetDocumentation( DocumentedNode node, Optional< Scanner.Token > forwardDocToken )
		throws IOException {
		if( node != null ) {
			// <Java 8>
			Optional< Scanner.Token > backwardDoc = parseBackwardDocumentation();
			if( backwardDoc.isPresent() ) {
				node.setDocumentation( backwardDoc.get().content() );
			} else {
				String forwardDoc = forwardDocToken
					.orElse( new Scanner.Token( Scanner.TokenType.DOCUMENTATION_FORWARD, "" ) ).content();
				node.setDocumentation( forwardDoc );
			}
			// </Java 8>

			/*
			 * Java 9 version of the above parseBackwardDocumentation().ifPresentOrElse( doc ->
			 * node.setDocumentation( doc.content() ), () -> node.setDocumentation( (forwardDocToken.orElse( new
			 * Scanner.Token( Scanner.TokenType.DOCUMENTATION_FORWARD, "" ) )).content() ) );
			 */
		} else {
			forwardDocToken.ifPresent( this::addToken );
			addToken( token );
			nextToken();
		}
	}

	private void parseInterfaceOrPort()
		throws IOException, ParserException {
		Optional< Scanner.Token > forwardDocToken = parseForwardDocumentation();

		final DocumentedNode node;
		if( token.isKeyword( "interface" ) ) {
			nextToken();
			if( token.isKeyword( "extender" ) ) {
				nextToken();
				node = parseInterfaceExtender();
			} else {
				node = parseInterface();
			}
		} else if( token.isKeyword( "inputPort" ) ) {
			node = parsePort();
		} else if( token.isKeyword( "outputPort" ) ) {
			node = parsePort();
		} else {
			node = null;
		}

		parseBackwardAndSetDocumentation( node, forwardDocToken );
	}

	private OutputPortInfo createInternalServicePort( String name, List< InterfaceDefinition > interfaceList )
		throws ParserException {
		OutputPortInfo p = new OutputPortInfo( getContext(), name );

		for( InterfaceDefinition interfaceDefinition : interfaceList ) {
			interfaceDefinition.copyTo( p );
		}
		return p;
	}

	private InputPortInfo createInternalServiceInputPort( String serviceName,
		List< InterfaceDefinition > interfaceList )
		throws ParserException {
		OLSyntaxNode protocolConfiguration = new NullProcessStatement( getContext() );
		InputPortInfo iport = null;
		try {
			iport = new InputPortInfo(
				getContext(),
				serviceName + "InputPort", // input port name
				new URI( Constants.LOCAL_LOCATION_KEYWORD ),
				null,
				protocolConfiguration,
				new InputPortInfo.AggregationItemInfo[] {},
				Collections.emptyMap() );

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
		throws IOException, ParserException {
		// only proceed if a service declaration
		if( !token.isKeyword( "service" ) ) {
			return;
		}

		// get service name
		nextToken();
		assertToken( Scanner.TokenType.ID, "expected service name" );
		String internalServiceName = token.content();

		// validate token
		nextToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );

		// initialize internal interface and interface list
		List< InterfaceDefinition > interfaceList = new ArrayList<>();

		OLSyntaxNode internalMain = null;
		SequenceStatement internalInit = null;

		boolean keepRunning = true;
		while( keepRunning ) {
			if( token.isKeyword( "Interfaces" ) ) {
				nextToken();
				eat( Scanner.TokenType.COLON, "expected : after Interfaces" );
				boolean keepRun = true;
				while( keepRun ) {
					assertToken( Scanner.TokenType.ID, "expected interface name" );
					InterfaceDefinition i = interfaces.get( token.content() );
					if( i == null ) {
						throwException( "Invalid interface name: " + token.content() );
					}
					interfaceList.add( i );
					nextToken();

					if( token.is( Scanner.TokenType.COMMA ) ) {
						nextToken();
					} else {
						keepRun = false;
					}
				}
			} else if( token.isKeyword( "main" ) ) {
				if( internalMain != null ) {
					throwException( "you must specify only one main definition" );
				}

				internalMain = parseMain();
			} else if( token.is( Scanner.TokenType.INIT ) ) {
				if( internalInit == null ) {
					internalInit = new SequenceStatement( getContext() );
				}

				internalInit.addChild( parseInit() );
			} else if( token.is( Scanner.TokenType.RCURLY ) ) {
				keepRunning = false;
			} else {
				throwException( "Unrecognized token in inline service." );
			}
		}

		// validate ending
		eat( Scanner.TokenType.RCURLY, "} expected" );

		// main in service needs to be defined
		if( internalMain == null ) {
			throwException( "You must specify a main for internal service " + internalServiceName );
		}

		// add output port to main program
		programBuilder.addChild( createInternalServicePort( internalServiceName, interfaceList ) );

		// create Program representing the internal service
		ProgramBuilder internalServiceProgramBuilder = new ProgramBuilder( getContext() );

		// copy children of parent to embedded service
		for( OLSyntaxNode child : programBuilder.children() ) {
			if( child instanceof InterfaceDefinition
				|| child instanceof OutputPortInfo
				|| child instanceof TypeDefinition ) {
				internalServiceProgramBuilder.addChild( child );
			}
		}

		// set execution to always concurrent
		internalServiceProgramBuilder.addChild( new ExecutionInfo( getContext(), Constants.ExecutionMode.CONCURRENT ) );

		// add input port to internal service
		internalServiceProgramBuilder.addChild( createInternalServiceInputPort( internalServiceName, interfaceList ) );

		// add init if defined in internal service
		if( internalInit != null ) {
			internalServiceProgramBuilder.addChild( new DefinitionNode( getContext(), "init", internalInit ) );
		}

		// add main defined in internal service
		internalServiceProgramBuilder.addChild( internalMain );

		// create internal embedded service node
		EmbeddedServiceNode internalServiceNode = new EmbeddedServiceNode( getContext(),
			Constants.EmbeddedServiceType.INTERNAL, internalServiceName, internalServiceName );

		// add internal service program to embedded service node
		internalServiceNode.setProgram( internalServiceProgramBuilder.toProgram() );

		// add embedded service node to program that is embedding it
		programBuilder.addChild( internalServiceNode );
	}

	private InputPortInfo parseInputPortInfo()
		throws IOException, ParserException {
		String inputPortName;
		String protocolId;
		URI inputPortLocation;
		List< InterfaceDefinition > interfaceList = new ArrayList<>();
		OLSyntaxNode protocolConfiguration = new NullProcessStatement( getContext() );

		nextToken();
		assertToken( Scanner.TokenType.ID, "expected inputPort name" );
		inputPortName = token.content();
		nextToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		InterfaceDefinition iface = new InterfaceDefinition( getContext(), "Internal interface for: " + inputPortName );

		inputPortLocation = null;
		protocolId = null;
		Map< String, String > redirectionMap = new HashMap<>();
		List< InputPortInfo.AggregationItemInfo > aggregationList = new ArrayList<>();
		while( token.isNot( Scanner.TokenType.RCURLY ) ) {
			if( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( iface );
			} else if( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( iface );
			} else if( token.isKeyword( "location" ) || token.isKeyword( "Location" ) ) {
				if( inputPortLocation != null ) {
					throwException( "Location already defined for service " + inputPortName );
				}
				nextToken();
				eat( Scanner.TokenType.COLON, "expected : after location" );
				checkConstant();
				assertToken( Scanner.TokenType.STRING, "expected inputPort location string" );
				try {
					inputPortLocation = new URI( token.content() );
				} catch( URISyntaxException e ) {
					throwException( e );
				}
				nextToken();
			} else if( token.isKeyword( "interfaces" ) || token.isKeyword( "Interfaces" ) ) {
				nextToken();
				eat( Scanner.TokenType.COLON, "expected : after interfaces" );
				boolean keepRun = true;
				while( keepRun ) {
					assertToken( Scanner.TokenType.ID, "expected interface name" );
					InterfaceDefinition i = interfaces.get( token.content() );
					if( i == null ) {
						throwException( "Invalid interface name: " + token.content() );
					}
					i.copyTo( iface );
					interfaceList.add( i );
					nextToken();

					if( token.is( Scanner.TokenType.COMMA ) ) {
						nextToken();
					} else {
						keepRun = false;
					}
				}
			} else if( token.isKeyword( "protocol" ) || token.isKeyword( "Protocol" ) ) {
				if( protocolId != null ) {
					throwException( "Protocol already defined for inputPort " + inputPortName );
				}
				nextToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				checkConstant();
				assertToken( Scanner.TokenType.ID, "expected protocol identifier" );
				protocolId = token.content();
				nextToken();
				if( token.is( Scanner.TokenType.LCURLY ) ) {
					addTokens( Arrays.asList(
						new Scanner.Token( Scanner.TokenType.ID, Constants.GLOBAL ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, Constants.INPUT_PORTS_NODE_NAME ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, inputPortName ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, Constants.PROTOCOL_NODE_NAME ),
						new Scanner.Token( Scanner.TokenType.DEEP_COPY_WITH_LINKS_LEFT ),
						token ) );
					// Protocol configuration
					nextToken();
					// protocolConfiguration = parseInVariablePathProcess( false );
					protocolConfiguration = parseBasicStatement();
				}
			} else if( token.isKeyword( "redirects" ) || token.isKeyword( "Redirects" ) ) {
				nextToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				String subLocationName;
				while( token.is( Scanner.TokenType.ID ) ) {
					subLocationName = token.content();
					nextToken();
					eat( Scanner.TokenType.ARROW, "expected =>" );
					assertToken( Scanner.TokenType.ID, "expected outputPort identifier" );
					redirectionMap.put( subLocationName, token.content() );
					nextToken();
					if( token.is( Scanner.TokenType.COMMA ) ) {
						nextToken();
					} else {
						break;
					}
				}
			} else if( token.isKeyword( "aggregates" ) || token.isKeyword( "Aggregates" ) ) {
				nextToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				parseAggregationList( aggregationList );
			} else {
				throwException( "Unrecognized token in inputPort " + inputPortName );
			}
		}
		eat( Scanner.TokenType.RCURLY, "} expected" );
		if( inputPortLocation == null ) {
			throwException( "expected location URI for " + inputPortName );
		} else if( iface.operationsMap().isEmpty() && redirectionMap.isEmpty() && aggregationList.isEmpty() ) {
			throwException( "expected at least one operation, interface, aggregation or redirection for inputPort "
				+ inputPortName );
		} else if( protocolId == null && !inputPortLocation.toString().equals( Constants.LOCAL_LOCATION_KEYWORD )
			&& !inputPortLocation.getScheme().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			throwException( "expected protocol for inputPort " + inputPortName );
		}
		InputPortInfo iport =
			new InputPortInfo( getContext(), inputPortName, inputPortLocation, protocolId, protocolConfiguration,
				aggregationList.toArray( new InputPortInfo.AggregationItemInfo[ 0 ] ),
				redirectionMap );
		for( InterfaceDefinition i : interfaceList ) {
			iport.addInterface( i );
		}
		iface.copyTo( iport );
		programBuilder.addChild( iport );
		return iport;
	}

	private void parseAggregationList( List< InputPortInfo.AggregationItemInfo > aggregationList )
		throws ParserException, IOException {
		List< String > outputPortNames;
		InterfaceExtenderDefinition extender;
		boolean mainKeepRun = true;
		while( mainKeepRun ) {
			extender = null;
			outputPortNames = new LinkedList<>();
			if( token.is( Scanner.TokenType.LCURLY ) ) {
				nextToken();
				boolean keepRun = true;
				while( keepRun ) {
					assertToken( Scanner.TokenType.ID, "expected output port name" );
					outputPortNames.add( token.content() );
					nextToken();
					if( token.is( Scanner.TokenType.COMMA ) ) {
						nextToken();
					} else if( token.is( Scanner.TokenType.RCURLY ) ) {
						keepRun = false;
						nextToken();
					} else {
						throwException( "unexpected token " + token.type() );
					}
				}
			} else {
				assertToken( Scanner.TokenType.ID, "expected output port name" );
				outputPortNames.add( token.content() );
				nextToken();
			}
			if( token.is( Scanner.TokenType.WITH ) ) {
				nextToken();
				assertToken( Scanner.TokenType.ID, "expected interface extender name" );
				extender = interfaceExtenders.get( token.content() );
				if( extender == null ) {
					throwException( "undefined interface extender: " + token.content() );
				}
				nextToken();
			}
			aggregationList.add( new InputPortInfo.AggregationItemInfo(
				outputPortNames.toArray( new String[ 0 ] ),
				extender ) );

			if( token.is( Scanner.TokenType.COMMA ) ) {
				nextToken();
			} else {
				mainKeepRun = false;
			}
		}
	}

	private InterfaceDefinition parseInterfaceExtender()
		throws IOException, ParserException {
		String name;
		assertToken( Scanner.TokenType.ID, "expected interface extender name" );
		name = token.content();
		nextToken();
		eat( Scanner.TokenType.LCURLY, "expected {" );
		InterfaceExtenderDefinition extender = currInterfaceExtender =
			new InterfaceExtenderDefinition( getContext(), name );
		parseOperations( currInterfaceExtender );
		interfaceExtenders.put( name, extender );
		programBuilder.addChild( currInterfaceExtender );
		eat( Scanner.TokenType.RCURLY, "expected }" );
		currInterfaceExtender = null;
		return extender;
	}

	private InterfaceDefinition parseInterface()
		throws IOException, ParserException {
		String name;
		InterfaceDefinition iface;
		assertToken( Scanner.TokenType.ID, "expected interface name" );
		name = token.content();
		nextToken();
		eat( Scanner.TokenType.LCURLY, "expected {" );
		iface = new InterfaceDefinition( getContext(), name );
		parseOperations( iface );
		interfaces.put( name, iface );
		programBuilder.addChild( iface );
		eat( Scanner.TokenType.RCURLY, "expected }" );

		return iface;
	}

	private void parseOperations( OperationCollector oc )
		throws IOException, ParserException {
		boolean keepRun = true;
		while( keepRun ) {

			if( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( oc );
			} else if( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( oc );
			} else {
				keepRun = false;
			}

		}
	}

	private void parseOutputPortInfo( OutputPortInfo p )
		throws IOException, ParserException {
		boolean keepRun = true;
		while( keepRun ) {
			if( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( p );
			} else if( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( p );
			} else if( token.isKeyword( "interfaces" ) || token.isKeyword( "Interfaces" ) ) {
				nextToken();
				eat( Scanner.TokenType.COLON, "expected : after Interfaces" );
				boolean r = true;
				while( r ) {
					assertToken( Scanner.TokenType.ID, "expected interface name" );
					InterfaceDefinition i = interfaces.get( token.content() );
					if( i == null ) {
						throwException( "Invalid interface name: " + token.content() );
					}
					i.copyTo( p );
					p.addInterface( i );
					nextToken();

					if( token.is( Scanner.TokenType.COMMA ) ) {
						nextToken();
					} else {
						r = false;
					}
				}
			} else if( token.isKeyword( "location" ) || token.isKeyword( "Location" ) ) {
				if( p.location() != null ) {
					throwException( "Location already defined for output port " + p.id() );
				}

				nextToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				checkConstant();

				assertToken( Scanner.TokenType.STRING, "expected location string" );
				URI location = null;
				try {
					location = new URI( token.content() );
				} catch( URISyntaxException e ) {
					throwException( e );
				}

				p.setLocation( location );
				nextToken();
			} else if( token.isKeyword( "protocol" ) || token.isKeyword( "Protocol" ) ) {
				if( p.protocolId() != null ) {
					throwException( "Protocol already defined for output port " + p.id() );
				}

				nextToken();
				eat( Scanner.TokenType.COLON, "expected :" );
				checkConstant();

				assertToken( Scanner.TokenType.ID, "expected protocol identifier" );
				p.setProtocolId( token.content() );
				nextToken();

				if( token.is( Scanner.TokenType.LCURLY ) ) {
					addTokens( Arrays.asList(
						new Scanner.Token( Scanner.TokenType.ID, p.id() ),
						new Scanner.Token( Scanner.TokenType.DOT ),
						new Scanner.Token( Scanner.TokenType.ID, "protocol" ),
						new Scanner.Token( Scanner.TokenType.DEEP_COPY_WITH_LINKS_LEFT ),
						token ) );
					// Protocol configuration
					nextToken();
					p.setProtocolConfiguration( parseBasicStatement() );
				}
			} else {
				keepRun = false;
			}

		}
	}

	private void parseOneWayOperations( OperationCollector oc )
		throws IOException, ParserException {
		nextToken();
		eat( Scanner.TokenType.COLON, "expected :" );

		boolean keepRun = true;
		Scanner.Token commentToken = null;
		String opId;
		while( keepRun ) {
			checkConstant();
			if( token.is( Scanner.TokenType.DOCUMENTATION_FORWARD ) ) {
				commentToken = token;
				nextToken();
			} else if( token.is( Scanner.TokenType.ID )
				|| (currInterfaceExtender != null && token.is( Scanner.TokenType.ASTERISK )) ) {
				opId = token.content();
				OneWayOperationDeclaration opDecl = new OneWayOperationDeclaration( getContext(), opId );
				nextToken();
				opDecl.setRequestType( TypeDefinitionUndefined.getInstance() );
				if( token.is( Scanner.TokenType.LPAREN ) ) { // Type declaration
					nextToken(); // eat (
					if( definedTypes.containsKey( token.content() ) == false ) {
						throwException( "invalid type: " + token.content() );
					}
					opDecl.setRequestType( definedTypes.get( token.content() ) );
					nextToken(); // eat the type name
					eat( Scanner.TokenType.RPAREN, "expected )" );
				}

				parseBackwardAndSetDocumentation( opDecl, Optional.ofNullable( commentToken ) );
				commentToken = null;

				if( currInterfaceExtender != null && opId.equals( "*" ) ) {
					currInterfaceExtender.setDefaultOneWayOperation( opDecl );
				} else {
					oc.addOperation( opDecl );
				}

				if( token.is( Scanner.TokenType.COMMA ) ) {
					nextToken();
				} else {
					keepRun = false;
				}
			} else {
				keepRun = false;
			}

		}
	}

	private void parseRequestResponseOperations( OperationCollector oc )
		throws IOException, ParserException {
		nextToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		boolean keepRun = true;
		Scanner.Token commentToken = null;
		String opId;
		while( keepRun ) {
			checkConstant();
			if( token.is( Scanner.TokenType.DOCUMENTATION_FORWARD ) ) {
				commentToken = token;
				nextToken();
			} else if( token.is( Scanner.TokenType.ID )
				|| (currInterfaceExtender != null && token.is( Scanner.TokenType.ASTERISK )) ) {
				opId = token.content();
				nextToken();
				String requestTypeName = TypeDefinitionUndefined.UNDEFINED_KEYWORD;
				String responseTypeName = TypeDefinitionUndefined.UNDEFINED_KEYWORD;

				if( token.is( Scanner.TokenType.LPAREN ) ) {
					nextToken(); // eat (
					requestTypeName = token.content();
					nextToken();
					eat( Scanner.TokenType.RPAREN, "expected )" );
					eat( Scanner.TokenType.LPAREN, "expected (" );
					responseTypeName = token.content();
					nextToken();
					eat( Scanner.TokenType.RPAREN, "expected )" );
				}

				Map< String, TypeDefinition > faultTypesMap = new HashMap<>();

				if( token.is( Scanner.TokenType.THROWS ) ) {
					nextToken();
					while( token.is( Scanner.TokenType.ID ) ) {
						String faultName = token.content();
						String faultTypeName = TypeDefinitionUndefined.UNDEFINED_KEYWORD;
						nextToken();
						if( token.is( Scanner.TokenType.LPAREN ) ) {
							nextToken(); // eat (
							faultTypeName = token.content();
							if( definedTypes.containsKey( faultTypeName ) == false ) {
								throwException( "invalid type: " + faultTypeName );
							}
							nextToken();
							eat( Scanner.TokenType.RPAREN, "expected )" );
						}
						faultTypesMap.put( faultName, definedTypes.get( faultTypeName ) );
					}
				}

				if( definedTypes.containsKey( requestTypeName ) == false ) {
					throwException( "invalid type: " + requestTypeName );
				}

				if( definedTypes.containsKey( responseTypeName ) == false ) {
					throwException( "invalid type: " + responseTypeName );
				}

				RequestResponseOperationDeclaration opRR =
					new RequestResponseOperationDeclaration(
						getContext(),
						opId,
						definedTypes.get( requestTypeName ),
						definedTypes.get( responseTypeName ),
						faultTypesMap );

				parseBackwardAndSetDocumentation( opRR, Optional.ofNullable( commentToken ) );
				commentToken = null;

				if( currInterfaceExtender != null && opId.equals( "*" ) ) {
					currInterfaceExtender.setDefaultRequestResponseOperation( opRR );
				} else {
					oc.addOperation( opRR );
				}

				if( token.is( Scanner.TokenType.COMMA ) ) {
					nextToken();
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
		throws IOException, ParserException {
		boolean keepRun = true;

		do {
			if( token.is( Scanner.TokenType.DEFINE ) ) {
				programBuilder.addChild( parseDefinition() );
			} else if( token.isKeyword( "courier" ) ) {
				programBuilder.addChild( parseCourierDefinition() );
			} else if( token.isKeyword( "main" ) ) {
				if( main != null ) {
					throwException( "you must specify only one main definition" );
				}

				main = parseMain();
			} else if( token.is( Scanner.TokenType.INIT ) ) {
				if( initSequence == null ) {
					initSequence = new SequenceStatement( getContext() );
				}

				initSequence.addChild( parseInit() );
			} else {
				keepRun = false;
			}

		} while( keepRun );
	}

	private DefinitionNode parseMain()
		throws IOException, ParserException {
		nextToken();
		eat( Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		DefinitionNode retVal = new DefinitionNode( getContext(), "main", parseProcess() );
		eat( Scanner.TokenType.RCURLY, "expected } after procedure definition" );
		return retVal;
	}

	private OLSyntaxNode parseInit()
		throws IOException, ParserException {
		nextToken();
		eat(
			Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		OLSyntaxNode retVal = parseProcess();
		eat(
			Scanner.TokenType.RCURLY, "expected } after procedure definition" );
		return retVal;
	}

	private DefinitionNode parseDefinition()
		throws IOException, ParserException {
		nextToken();
		assertToken( Scanner.TokenType.ID, "expected definition identifier" );
		String definitionId = token.content();
		nextToken();

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
		throws IOException, ParserException {
		nextToken();
		assertToken( Scanner.TokenType.ID, "expected input port identifier" );
		String inputPortName = token.content();
		nextToken();

		eat( Scanner.TokenType.LCURLY, "expected { after courier definition" );
		CourierDefinitionNode retVal = new CourierDefinitionNode(
			getContext(),
			inputPortName,
			parseCourierChoice() );
		eat( Scanner.TokenType.RCURLY, "expected } after courier definition" );

		return retVal;
	}

	public OLSyntaxNode parseProcess()
		throws IOException, ParserException {
		return parseParallelStatement();
	}

	private ParallelStatement parseParallelStatement()
		throws IOException, ParserException {
		ParallelStatement stm = new ParallelStatement( getContext() );
		stm.addChild( parseSequenceStatement() );
		while( token.is( Scanner.TokenType.PARALLEL ) ) {
			nextToken();
			stm.addChild( parseSequenceStatement() );
		}

		return stm;
	}

	private SequenceStatement parseSequenceStatement()
		throws IOException, ParserException {
		SequenceStatement stm = new SequenceStatement( getContext() );

		stm.addChild( parseBasicStatement() );
		boolean run = true;
		while( run ) {
			if( token.is( Scanner.TokenType.SEQUENCE ) ) {
				nextToken();
				stm.addChild( parseBasicStatement() );
			} else if( hasMetNewline() ) {
				OLSyntaxNode basicStatement = parseBasicStatement( false );
				if( basicStatement == null ) {
					run = false;
				} else {
					stm.addChild( basicStatement );
				}
			} else {
				run = false;
			}
		}

		return stm;
	}

	private final List< List< Scanner.Token > > inVariablePaths = new ArrayList<>();

	private OLSyntaxNode parseInVariablePathProcess( boolean withConstruct )
		throws IOException, ParserException {
		OLSyntaxNode ret;
		LinkedList< Scanner.Token > tokens = new LinkedList<>();

		try {
			if( withConstruct ) {
				eat( Scanner.TokenType.LPAREN, "expected (" );
				while( token.isNot( Scanner.TokenType.LCURLY ) ) {
					tokens.add( token );
					nextTokenNotEOF();
				}
				// TODO transfer this whole buggy thing to the OOIT
				tokens.removeLast();
				// getToken();
			} else {
				while( token.isNot( Scanner.TokenType.LCURLY ) ) {
					tokens.add( token );
					nextTokenNotEOF();
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
		throws IOException, ParserException {
		return parseBasicStatement( true );
	}

	private OLSyntaxNode parseBasicStatement( boolean throwException )
		throws IOException, ParserException {
		OLSyntaxNode retVal = null;

		switch( token.type() ) {
		case LSQUARE:
			retVal = parseNDChoiceStatement();
			break;
		case PROVIDE:
			nextToken();
			retVal = parseProvideUntilStatement();
			break;
		case ID:
			checkConstant();
			String id = token.content();
			nextToken();

			if( token.is( Scanner.TokenType.LSQUARE ) || token.is( Scanner.TokenType.DOT )
				|| token.is( Scanner.TokenType.ASSIGN ) || token.is( Scanner.TokenType.ADD_ASSIGN )
				|| token.is( Scanner.TokenType.MINUS_ASSIGN ) || token.is( Scanner.TokenType.MULTIPLY_ASSIGN )
				|| token.is( Scanner.TokenType.DIVIDE_ASSIGN ) || token.is( Scanner.TokenType.POINTS_TO )
				|| token.is( Scanner.TokenType.DEEP_COPY_LEFT )
				|| token.is( Scanner.TokenType.DEEP_COPY_WITH_LINKS_LEFT ) || token.is( Scanner.TokenType.DECREMENT )
				|| token.is( Scanner.TokenType.INCREMENT ) ) {
				retVal = parseAssignOrDeepCopyOrPointerStatement( _parseVariablePath( id ) );
			} else if( id.equals( "forward" )
				&& (token.is( Scanner.TokenType.ID ) || token.is( Scanner.TokenType.LPAREN )) ) {
				retVal = parseForwardStatement();
			} else if( token.is( Scanner.TokenType.LPAREN ) ) {
				retVal = parseInputOperationStatement( id );
			} else if( token.is( Scanner.TokenType.AT ) ) {
				nextToken();
				retVal = parseOutputOperationStatement( id );
			} else {
				retVal = new DefinitionCallStatement( getContext(), id );
			}
			break;
		case WITH:
			nextToken();
			retVal = parseInVariablePathProcess( true );
			break;
		case INCREMENT:
			nextToken();
			retVal = new PreIncrementStatement( getContext(), parseVariablePath() );
			break;
		case DECREMENT:
			nextToken();
			retVal = new PreDecrementStatement( getContext(), parseVariablePath() );
			break;
		case UNDEF:
			nextToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();
			retVal =
				new UndefStatement( getContext(), parseVariablePath() );
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			break;
		case SYNCHRONIZED:
			nextToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			assertToken( Scanner.TokenType.ID, "expected lock id" );
			final String sid = token.content();
			nextToken();

			eat( Scanner.TokenType.RPAREN, "expected )" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			retVal = new SynchronizedStatement( getContext(), sid, parseProcess() );
			eat( Scanner.TokenType.RCURLY, "expected }" );
			break;
		case SPAWN:
			nextToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			VariablePathNode indexVariablePath = parseVariablePath();
			assertToken( Scanner.TokenType.ID, "expected over" );
			if( token.isKeyword( "over" ) == false ) {
				throwException( "expected over" );
			}
			nextToken();
			OLSyntaxNode upperBoundExpression = parseBasicExpression();
			eat( Scanner.TokenType.RPAREN, "expected )" );

			VariablePathNode inVariablePath = null;
			if( token.isKeyword( "in" ) ) {
				nextToken();
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
				process );
			break;
		case FOR:
			nextToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );

			startBackup();
			VariablePathNode leftPath;

			try {
				leftPath = parseVariablePath();
			} catch( ParserException e ) {
				leftPath = null;
			}

			if( leftPath != null && token.isKeyword( "in" ) ) {
				// for( elem in path ) { ... }
				discardBackup();

				nextToken();
				VariablePathNode targetPath = parseVariablePath();
				if( targetPath.path().get( targetPath.path().size() - 1 ).value() != null ) {
					throwException(
						"target in for ( elem -> array ) { ... } should be an array (cannot specify an index): "
							+ targetPath.toPrettyString() );
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
			nextToken();
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
			nextToken();
			retVal =
				new CurrentHandlerStatement( getContext() );
			break;
		case NULL_PROCESS:
			nextToken();
			retVal =
				new NullProcessStatement( getContext() );
			break;
		case EXIT:
			nextToken();
			retVal =
				new ExitStatement( getContext() );
			break;
		case WHILE:
			retVal = parseWhileStatement();
			break;
		case LINKOUT:
			nextToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			assertToken(
				Scanner.TokenType.ID, "expected link identifier" );
			retVal =
				new LinkOutStatement( getContext(), token.content() );
			nextToken();

			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			break;
		case LPAREN:
			nextToken();
			retVal =
				parseProcess();
			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			break;
		case LCURLY:
			nextToken();
			retVal =
				parseProcess();
			eat(
				Scanner.TokenType.RCURLY, "expected }" );
			break;
		case SCOPE:
			nextToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected scope identifier" );
			final String scopeId = token.content();
			nextToken();

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
			nextToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected scope identifier" );
			retVal =
				new CompensateStatement( getContext(), token.content() );
			nextToken();

			eat(
				Scanner.TokenType.RPAREN, "expected )" );
			break;
		case THROW:
			nextToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();

			assertToken(
				Scanner.TokenType.ID, "expected fault identifier" );
			String faultName = token.content();
			nextToken();

			if( token.is( Scanner.TokenType.RPAREN ) ) {
				retVal = new ThrowStatement( getContext(), faultName );
			} else {
				eat( Scanner.TokenType.COMMA, "expected , or )" );
				OLSyntaxNode expression = parseExpression();
				/*
				 * assertToken( Scanner.TokenType.ID, "expected variable path" ); String varId = token.content();
				 * getToken(); VariablePathNode path = parseVariablePath( varId );
				 */
				retVal =
					new ThrowStatement( getContext(), faultName, expression );
			}

			eat( Scanner.TokenType.RPAREN, "expected )" );
			break;
		case INSTALL:
			nextToken();
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

			nextToken();

			eat( Scanner.TokenType.LPAREN, "expected (" );
			cond = parseExpression();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			node = parseBasicStatement();
			stm.addChild( new Pair<>( cond, node ) );

			boolean keepRun = true;
			while( token.is( Scanner.TokenType.ELSE ) && keepRun ) {
				nextToken();
				if( token.is( Scanner.TokenType.IF ) ) { // else if branch
					nextToken();
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
			if( !inVariablePaths.isEmpty() ) {
				retVal = parseAssignOrDeepCopyOrPointerStatement( parsePrefixedVariablePath() );
			}
			break;
		default:
			break;
		}

		if( throwException && retVal == null ) {
			throwException( "expected basic statement" );
		}

		return retVal;
	}

	private OLSyntaxNode parseProvideUntilStatement()
		throws IOException, ParserException {
		ParsingContext context = getContext();
		NDChoiceStatement provide = parseNDChoiceStatement();
		if( !token.isKeyword( "until" ) ) {
			throwException( "expected until" );
		}
		nextToken();

		NDChoiceStatement until = parseNDChoiceStatement();

		return new ProvideUntilStatement( context, provide, until );
	}

	private OLSyntaxNode parseForwardStatement()
		throws IOException, ParserException {
		OLSyntaxNode retVal;
		String outputPortName = null;
		if( token.is( Scanner.TokenType.ID ) ) {
			outputPortName = token.content();
			nextToken();
		}
		VariablePathNode outputVariablePath = parseOperationVariablePathParameter();
		if( token.is( Scanner.TokenType.LPAREN ) ) { // Solicit-Response
			VariablePathNode inputVariablePath = parseOperationVariablePathParameter();
			retVal = new SolicitResponseForwardStatement( getContext(), outputPortName, outputVariablePath,
				inputVariablePath );
		} else { // Notification
			retVal = new NotificationForwardStatement( getContext(), outputPortName, outputVariablePath );
		}

		return retVal;
	}

	private InstallFunctionNode parseInstallFunction()
		throws IOException, ParserException {
		boolean backup = insideInstallFunction;
		insideInstallFunction = true;
		List< Pair< String, OLSyntaxNode > > vec = new LinkedList<>();

		boolean keepRun = true;
		List< String > names = new ArrayList<>();
		OLSyntaxNode handler;
		while( keepRun ) {
			do {
				if( token.is( Scanner.TokenType.THIS ) ) {
					names.add( null );
				} else if( token.is( Scanner.TokenType.ID ) ) {
					names.add( token.content() );
				} else {
					throwException( "expected fault identifier or this" );
				}
				nextToken();
			} while( token.isNot( Scanner.TokenType.ARROW ) );
			nextToken(); // eat the arrow
			handler = parseProcess();
			for( String name : names ) {
				vec.add( new Pair<>( name, handler ) );
			}
			names.clear();

			if( token.is( Scanner.TokenType.COMMA ) ) {
				nextToken();
			} else {
				keepRun = false;
			}
		}

		insideInstallFunction = backup;

		return new InstallFunctionNode( vec.toArray( new Pair[ 0 ] ) );
	}

	private OLSyntaxNode parseAssignOrDeepCopyOrPointerStatement( VariablePathNode path )
		throws IOException, ParserException {
		OLSyntaxNode retVal = null;

		if( token.is( Scanner.TokenType.ASSIGN ) ) {
			nextToken();
			retVal =
				new AssignStatement( getContext(), path, parseExpression() );
		} else if( token.is( Scanner.TokenType.ADD_ASSIGN ) ) {
			nextToken();
			retVal =
				new AddAssignStatement( getContext(), path, parseExpression() );
		} else if( token.is( Scanner.TokenType.MINUS_ASSIGN ) ) {
			nextToken();
			retVal =
				new SubtractAssignStatement( getContext(), path, parseExpression() );
		} else if( token.is( Scanner.TokenType.MULTIPLY_ASSIGN ) ) {
			nextToken();
			retVal =
				new MultiplyAssignStatement( getContext(), path, parseExpression() );
		} else if( token.is( Scanner.TokenType.DIVIDE_ASSIGN ) ) {
			nextToken();
			retVal =
				new DivideAssignStatement( getContext(), path, parseExpression() );
		} else if( token.is( Scanner.TokenType.INCREMENT ) ) {
			nextToken();
			retVal =
				new PostIncrementStatement( getContext(), path );
		} else if( token.is( Scanner.TokenType.DECREMENT ) ) {
			nextToken();
			retVal =
				new PostDecrementStatement( getContext(), path );
		} else if( token.is( Scanner.TokenType.POINTS_TO ) ) {
			nextToken();
			retVal =
				new PointerStatement( getContext(), path, parseVariablePath() );
		} else if( token.is( Scanner.TokenType.DEEP_COPY_LEFT ) ) {
			ParsingContext context = getContext();
			nextToken();
			retVal = new DeepCopyStatement( context, path, parseExpression(), false );
		} else if( token.is( Scanner.TokenType.DEEP_COPY_WITH_LINKS_LEFT ) ) {
			ParsingContext context = getContext();
			nextToken();
			retVal = new DeepCopyStatement( context, path, parseExpression(), true );
		} else {
			throwException( "expected = or -> or << or -- or ++" );
		}

		return retVal;
	}

	private VariablePathNode parseVariablePath()
		throws ParserException, IOException {
		if( token.is( Scanner.TokenType.DOT ) ) {
			return parsePrefixedVariablePath();
		}
		assertToken( Scanner.TokenType.ID, "Expected variable path" );
		String varId = token.content();
		nextToken();
		return _parseVariablePath( varId );
	}

	private VariablePathNode _parseVariablePath( String varId )
		throws IOException, ParserException {
		OLSyntaxNode expr;
		VariablePathNode path;

		switch( varId ) {
		case Constants.GLOBAL:
			path = new VariablePathNode( getContext(), Type.GLOBAL );
			break;
		case Constants.CSETS:
			path = new VariablePathNode( getContext(), Type.CSET );
			path.append( new Pair<>( new ConstantStringExpression( getContext(), varId ),
				new ConstantIntegerExpression( getContext(), 0 ) ) );
			break;
		default:
			path = new VariablePathNode( getContext(), Type.NORMAL );
			if( token.is( Scanner.TokenType.LSQUARE ) ) {
				nextToken();
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
		while( token.is( Scanner.TokenType.DOT ) ) {
			nextToken();
			if( token.isIdentifier() ) {
				nodeExpr = new ConstantStringExpression( getContext(), token.content() );
			} else if( token.is( Scanner.TokenType.LPAREN ) ) {
				nextToken();
				nodeExpr = parseBasicExpression();
				assertToken(
					Scanner.TokenType.RPAREN, "expected )" );
			} else {
				throwException( "expected nested node identifier" );
			}

			nextToken();
			if( token.is( Scanner.TokenType.LSQUARE ) ) {
				nextToken();
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
		throws IOException, ParserException {
		int i = inVariablePaths.size() - 1;
		List< Scanner.Token > tokens = new ArrayList<>();
		try {
			tokens.addAll( inVariablePaths.get( i ) );
		} catch( IndexOutOfBoundsException e ) {
			throwException( "Prefixed variable paths must be inside a with block" );
		}

		while( tokens.get( 0 ).is( Scanner.TokenType.DOT ) ) {
			i--;
			tokens.addAll( 0, inVariablePaths.get( i ) );
		}

		addTokens( tokens );
		addTokens( Collections.singletonList( new Scanner.Token( Scanner.TokenType.DOT ) ) );
		nextToken();

		String varId = token.content();
		nextToken();

		return _parseVariablePath( varId );
	}

	private CourierChoiceStatement parseCourierChoice()
		throws IOException, ParserException {
		CourierChoiceStatement stm = new CourierChoiceStatement( getContext() );
		OLSyntaxNode body;
		InterfaceDefinition iface;
		String operationName;
		VariablePathNode inputVariablePath, outputVariablePath;

		while( token.is( Scanner.TokenType.LSQUARE ) ) {
			iface = null;
			operationName = null;
			inputVariablePath = null;
			outputVariablePath = null;
			nextToken();
			if( token.isKeyword( "interface" ) ) {
				nextToken();
				assertToken( Scanner.TokenType.ID, "expected interface name" );
				checkConstant();
				iface = interfaces.get( token.content() );
				if( iface == null ) {
					throwException( "undefined interface: " + token.content() );
				}
				nextToken();
				inputVariablePath = parseOperationVariablePathParameter();
				if( inputVariablePath == null ) {
					throwException( "expected variable path" );
				}
				if( token.is( Scanner.TokenType.LPAREN ) ) { // Request-Response
					outputVariablePath = parseOperationVariablePathParameter();
				}
			} else if( token.is( Scanner.TokenType.ID ) ) {
				operationName = token.content();
				nextToken();
				inputVariablePath = parseOperationVariablePathParameter();
				if( inputVariablePath == null ) {
					throwException( "expected variable path" );
				}
				if( token.is( Scanner.TokenType.LPAREN ) ) { // Request-Response
					outputVariablePath = parseOperationVariablePathParameter();
				}
			} else {
				throwException( "expected courier input guard (interface or operation name)" );
			}

			eat( Scanner.TokenType.RSQUARE, "expected ]" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			body = parseProcess();
			eat( Scanner.TokenType.RCURLY, "expected }" );

			if( iface == null ) { // It's an operation
				if( outputVariablePath == null ) { // One-Way
					stm.operationOneWayBranches().add(
						new CourierChoiceStatement.OperationOneWayBranch( operationName, inputVariablePath, body ) );
				} else { // Request-Response
					stm.operationRequestResponseBranches()
						.add( new CourierChoiceStatement.OperationRequestResponseBranch( operationName,
							inputVariablePath, outputVariablePath, body ) );
				}
			} else { // It's an interface
				if( outputVariablePath == null ) { // One-Way
					stm.interfaceOneWayBranches()
						.add( new CourierChoiceStatement.InterfaceOneWayBranch( iface, inputVariablePath, body ) );
				} else { // Request-Response
					stm.interfaceRequestResponseBranches()
						.add( new CourierChoiceStatement.InterfaceRequestResponseBranch( iface, inputVariablePath,
							outputVariablePath, body ) );
				}
			}
		}

		return stm;
	}

	private NDChoiceStatement parseNDChoiceStatement()
		throws IOException, ParserException {
		NDChoiceStatement stm = new NDChoiceStatement( getContext() );
		OLSyntaxNode inputGuard = null;
		OLSyntaxNode process;

		while( token.is( Scanner.TokenType.LSQUARE ) ) {
			nextToken();
			// Eat [
			/*
			 * if ( token.is( Scanner.TokenType.LINKIN ) ) { inputGuard = parseLinkInStatement(); } else
			 */if( token.is( Scanner.TokenType.ID ) ) {
				String id = token.content();
				nextToken();
				inputGuard = parseInputOperationStatement( id );
			} else {
				throwException( "expected input guard" );
			}

			eat( Scanner.TokenType.RSQUARE, "expected ]" );
			if( token.is( Scanner.TokenType.LCURLY ) ) {
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
		throws IOException, ParserException {
		nextToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		assertToken( Scanner.TokenType.ID, "expected link identifier" );
		LinkInStatement stm = new LinkInStatement( getContext(), token.content() );
		nextToken();

		eat( Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}

	private OLSyntaxNode parseInputOperationStatement( String id )
		throws IOException, ParserException {
		ParsingContext context = getContext();
		VariablePathNode inputVarPath = parseOperationVariablePathParameter();
		OLSyntaxNode stm;

		if( token.is( Scanner.TokenType.LPAREN ) ) { // Request Response operation
			OLSyntaxNode outputExpression = parseOperationExpressionParameter();
			OLSyntaxNode process = new NullProcessStatement( getContext() );
			if( token.is( Scanner.TokenType.LCURLY ) ) { // Request Response body
				nextToken();
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
		throws IOException, ParserException {
		VariablePathNode ret = null;

		eat( Scanner.TokenType.LPAREN, "expected (" );
		if( token.is( Scanner.TokenType.ID ) ) {
			ret = parseVariablePath();
		} else if( token.is( Scanner.TokenType.DOT ) ) {
			ret = parsePrefixedVariablePath();
		}

		eat( Scanner.TokenType.RPAREN, "expected )" );

		return ret;
	}

	private OLSyntaxNode parseOperationExpressionParameter()
		throws IOException, ParserException {
		OLSyntaxNode ret = null;

		eat( Scanner.TokenType.LPAREN, "expected (" );
		if( token.isNot( Scanner.TokenType.RPAREN ) ) {
			ret = parseExpression();
		}

		eat( Scanner.TokenType.RPAREN, "expected )" );

		return ret;
	}

	private OLSyntaxNode parseOutputOperationStatement( String id )
		throws IOException, ParserException {
		ParsingContext context = getContext();
		String outputPortId = token.content();
		nextToken();

		OLSyntaxNode outputExpression = parseOperationExpressionParameter();

		OLSyntaxNode stm;

		if( token.is( Scanner.TokenType.LPAREN ) ) { // Solicit Response operation
			VariablePathNode inputVarPath = parseOperationVariablePathParameter();
			InstallFunctionNode function = null;
			if( token.is( Scanner.TokenType.LSQUARE ) ) {
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
		throws IOException, ParserException {
		ParsingContext context = getContext();
		OLSyntaxNode cond, process;
		nextToken();

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
		throws IOException, ParserException {
		OrConditionNode orCond = new OrConditionNode( getContext() );
		orCond.addChild( parseAndCondition() );
		while( token.is( Scanner.TokenType.OR ) ) {
			nextToken();
			orCond.addChild( parseAndCondition() );
		}

		return orCond;
	}

	private OLSyntaxNode parseAndCondition()
		throws IOException, ParserException {
		AndConditionNode andCond = new AndConditionNode( getContext() );
		andCond.addChild( parseBasicCondition() );
		while( token.is( Scanner.TokenType.AND ) ) {
			nextToken();
			andCond.addChild( parseBasicCondition() );
		}

		return andCond;
	}

	private OLSyntaxNode parseBasicCondition()
		throws IOException, ParserException {
		OLSyntaxNode ret;

		Scanner.TokenType opType;
		OLSyntaxNode expr1;

		expr1 = parseBasicExpression();

		opType = token.type();
		if( opType == Scanner.TokenType.EQUAL || opType == Scanner.TokenType.LANGLE ||
			opType == Scanner.TokenType.RANGLE || opType == Scanner.TokenType.MAJOR_OR_EQUAL ||
			opType == Scanner.TokenType.MINOR_OR_EQUAL || opType == Scanner.TokenType.NOT_EQUAL ) {
			OLSyntaxNode expr2;
			nextToken();
			expr2 = parseBasicExpression();
			ret = new CompareConditionNode( getContext(), expr1, expr2, opType );
		} else if( opType == Scanner.TokenType.INSTANCE_OF ) {
			nextToken();
			TypeDefinition type;
			NativeType nativeType = readNativeType();
			if( nativeType == null ) { // It's a user-defined type
				assertToken( Scanner.TokenType.ID, "expected type name after instanceof" );
			}
			if( definedTypes.containsKey( token.content() ) == false ) {
				throwException( "invalid type: " + token.content() );
			}
			type = definedTypes.get( token.content() );
			ret = new InstanceOfExpressionNode( getContext(), expr1, type );
			nextToken();
		} else {
			ret = expr1;
		}

		if( ret == null ) {
			throwException( "expected condition" );
		}

		return ret;
	}

	/*
	 * todo: Check if negative integer handling is appropriate
	 */
	private OLSyntaxNode parseBasicExpression()
		throws IOException, ParserException {
		boolean keepRun = true;
		SumExpressionNode sum = new SumExpressionNode( getContext() );
		sum.add( parseProductExpression() );

		while( keepRun ) {
			if( token.is( Scanner.TokenType.PLUS ) ) {
				nextToken();
				sum.add( parseProductExpression() );
			} else if( token.is( Scanner.TokenType.MINUS ) ) {
				nextToken();
				sum.subtract( parseProductExpression() );
			} else if( token.is( Scanner.TokenType.INT ) ) { // e.g. i -1
				int value = Integer.parseInt( token.content() );
				// We add it, because it's already negative.
				if( value < 0 ) {
					sum.add( parseProductExpression() );
				} else { // e.g. i 1
					throwException( "expected expression operator" );
				}
			} else if( token.is( Scanner.TokenType.LONG ) ) { // e.g. i -1L
				long value = Long.parseLong( token.content() );
				// We add it, because it's already negative.
				if( value < 0 ) {
					sum.add( parseProductExpression() );
				} else { // e.g. i 1
					throwException( "expected expression operator" );
				}
			} else if( token.is( Scanner.TokenType.DOUBLE ) ) { // e.g. i -1
				double value = Double.parseDouble( token.content() );
				// We add it, because it's already negative.
				if( value < 0 ) {
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
		throws IOException, ParserException {
		OLSyntaxNode retVal = null;
		VariablePathNode path = null;

		checkConstant();

		switch( token.type() ) {
		case ID:
			path = parseVariablePath();
			VariablePathNode freshValuePath = new VariablePathNode( getContext(), Type.NORMAL );
			freshValuePath.append( new Pair<>( new ConstantStringExpression( getContext(), "new" ),
				new ConstantIntegerExpression( getContext(), 0 ) ) );
			if( path.isEquivalentTo( freshValuePath ) ) {
				retVal = new FreshValueExpressionNode( path.context() );
				return retVal;
			}
			break;
		case DOT:
			path = parseVariablePath();
			break;
		case CARET:
			if( insideInstallFunction ) {
				nextToken();
				path = parseVariablePath();
				retVal = new InstallFixedVariableExpressionNode( getContext(), path );
				return retVal;
			}
			break;
		default:
			break;
		}

		if( path != null ) {
			switch( token.type() ) {
			case INCREMENT:
				nextToken();
				retVal =
					new PostIncrementStatement( getContext(), path );
				break;
			case DECREMENT:
				nextToken();
				retVal =
					new PostDecrementStatement( getContext(), path );
				break;
			case ASSIGN:
				nextToken();
				retVal = new AssignStatement( getContext(), path, parseExpression() );
				break;
			default:
				retVal = new VariableExpressionNode( getContext(), path );
				break;
			}
		} else {
			switch( token.type() ) {
			case NOT:
				nextToken();
				retVal = new NotExpressionNode( getContext(), parseFactor() );
				break;
			case STRING:
				retVal = new ConstantStringExpression( getContext(), token.content() );
				nextToken();
				break;
			case INT:
				retVal = new ConstantIntegerExpression( getContext(), Integer.parseInt( token.content() ) );
				nextToken();
				break;
			case LONG:
				retVal = new ConstantLongExpression( getContext(), Long.parseLong( token.content() ) );
				nextToken();
				break;
			case TRUE:
				retVal = new ConstantBoolExpression( getContext(), true );
				nextToken();
				break;
			case FALSE:
				retVal = new ConstantBoolExpression( getContext(), false );
				nextToken();
				break;
			case DOUBLE:
				retVal = new ConstantDoubleExpression( getContext(), Double.parseDouble( token.content() ) );
				nextToken();
				break;
			case LPAREN:
				nextToken();
				retVal = parseExpression();
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case HASH:
				nextToken();
				retVal = new ValueVectorSizeExpressionNode(
					getContext(),
					parseVariablePath() );
				break;
			case INCREMENT:
				nextToken();
				retVal = new PreIncrementStatement( getContext(), parseVariablePath() );
				break;
			case DECREMENT:
				nextToken();
				retVal = new PreDecrementStatement( getContext(), parseVariablePath() );
				break;
			case IS_DEFINED:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.DEFINED,
					parseVariablePath() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_INT:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.INT,
					parseVariablePath() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_DOUBLE:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.DOUBLE,
					parseVariablePath() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_BOOL:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.BOOL,
					parseVariablePath() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_LONG:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.LONG,
					parseVariablePath() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case IS_STRING:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new IsTypeExpressionNode(
					getContext(),
					IsTypeExpressionNode.CheckType.STRING,
					parseVariablePath() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_INT:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode( getContext(), NativeType.INT, parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_LONG:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode( getContext(), NativeType.LONG, parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_BOOL:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode( getContext(), NativeType.BOOL, parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_DOUBLE:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode(
					getContext(),
					NativeType.DOUBLE,
					parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			case CAST_STRING:
				nextToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				retVal = new TypeCastExpressionNode(
					getContext(),
					NativeType.STRING,
					parseExpression() );
				eat( Scanner.TokenType.RPAREN, "expected )" );
				break;
			default:
				break;
			}
		}

		if( retVal == null ) {
			if( token.is( Scanner.TokenType.LCURLY ) ) {
				retVal = new VoidExpressionNode( getContext() );
			} else {
				throwException( "expected expression" );
			}
		}

		if( token.is( Scanner.TokenType.LCURLY ) ) {
			retVal = parseInlineTreeExpression( retVal );
		}

		return retVal;
	}

	private OLSyntaxNode parseInlineTreeExpression( OLSyntaxNode rootExpression )
		throws IOException, ParserException {
		eat( Scanner.TokenType.LCURLY, "expected {" );

		VariablePathNode path;

		List< InlineTreeExpressionNode.Operation > operations = new ArrayList<>();

		while( !token.is( Scanner.TokenType.RCURLY ) ) {
			maybeEat( Scanner.TokenType.DOT );

			path = parseVariablePath();

			InlineTreeExpressionNode.Operation operation = null;
			switch( token.type() ) {
			case DEEP_COPY_LEFT:
				nextToken();
				operation = new InlineTreeExpressionNode.DeepCopyOperation( path, parseExpression() );
				break;
			case ASSIGN:
				nextToken();
				operation = new InlineTreeExpressionNode.AssignmentOperation( path, parseExpression() );
				break;
			case POINTS_TO:
				nextToken();
				operation = new InlineTreeExpressionNode.PointsToOperation( path, parseVariablePath() );
				break;
			default:
				throwException( "expected =, <<, or ->" );
				break;
			}

			operations.add( operation );

			maybeEat( Scanner.TokenType.COMMA, Scanner.TokenType.SEQUENCE );
		}

		eat( Scanner.TokenType.RCURLY, "expected }" );

		return new InlineTreeExpressionNode(
			rootExpression.context(),
			rootExpression,
			operations.toArray( new InlineTreeExpressionNode.Operation[ 0 ] ) );
	}

	private OLSyntaxNode parseProductExpression()
		throws IOException, ParserException {
		ProductExpressionNode product = new ProductExpressionNode( getContext() );
		product.multiply( parseFactor() );
		boolean keepRun = true;
		while( keepRun ) {
			switch( token.type() ) {
			case ASTERISK:
				nextToken();
				product.multiply( parseFactor() );
				break;
			case DIVIDE:
				nextToken();
				product.divide( parseFactor() );
				break;
			case PERCENT_SIGN:
				nextToken();
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

		private IncludeFile( InputStream inputStream, String parentPath, URI uri ) {
			this.inputStream = inputStream;
			this.parentPath = parentPath;
			this.uri = uri;
		}

		private InputStream getInputStream() {
			return inputStream;
		}

		private String getParentPath() {
			return parentPath;
		}

		private URI getURI() {
			return uri;
		}
	}
}
