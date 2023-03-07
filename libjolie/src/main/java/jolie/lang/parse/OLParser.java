/*
 * Copyright (C) 2006-2021 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk> 
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
import java.util.stream.Collectors;

//import jolie.lang.CodeCheckMessage;
import jolie.lang.Constants;
import jolie.lang.Keywords;
import jolie.lang.Constants.EmbeddedServiceType;
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
import jolie.lang.parse.ast.VariablePathNode.Type;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.types.BasicTypeDefinition;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinement;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementDoubleRanges;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementIntegerRanges;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementLongRanges;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementStringLength;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementStringList;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementStringRegex;
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
		void parse() throws IOException, ParserException;
	}

	private interface ParsingSupplier {
		OLSyntaxNode parse() throws IOException, ParserException;
	}

	private long faultIdCounter = 0;
	private final ProgramBuilder programBuilder;
	private final Map< String, Scanner.Token > constantsMap =
		new HashMap<>();
	private boolean insideInstallFunction = false;
	private String[] includePaths;
	private boolean hasIncludeDirective = false;
	private final Map< String, InterfaceExtenderDefinition > interfaceExtenders =
		new HashMap<>();

	private final Map< String, TypeDefinition > definedTypes;
	private final ClassLoader classLoader;

	private InterfaceExtenderDefinition currInterfaceExtender = null;

	private static enum RefinementPredicates {
		LENGTH, ENUM, RANGES, REGEX
	}

	private static final Map< String, OLParser.RefinementPredicates > BASIC_TYPE_REFINED_PREDICATES = new HashMap<>();
	static {
		BASIC_TYPE_REFINED_PREDICATES.put( "length", RefinementPredicates.LENGTH ); // defines the minimum and the
																					// maximum
																					// length of a string

		BASIC_TYPE_REFINED_PREDICATES.put( "regex", RefinementPredicates.REGEX ); // defines the regex for a string

		BASIC_TYPE_REFINED_PREDICATES.put( "enum", RefinementPredicates.ENUM ); // defines a list of string that a
																				// string
																				// can be
		BASIC_TYPE_REFINED_PREDICATES.put( "ranges", RefinementPredicates.RANGES ); // it defines a list of intervals
																					// where
																					// an int | long | double can be
	}


	public OLParser( Scanner scanner, String[] includePaths, ClassLoader classLoader ) {
		super( scanner );
		final ParsingContext context = new URIParsingContext( scanner.source(), 1, 1, 0, 0, List.of() );
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
				new TypeInlineDefinition( context, type.id(), BasicTypeDefinition.of( type ),
					Constants.RANGE_ONE_TO_ONE ) );
		}
		definedTypes.put( TypeDefinitionUndefined.UNDEFINED_KEYWORD, TypeDefinitionUndefined.getInstance() );

		return definedTypes;
	}

	public Program parse()
		throws IOException, ParserException {

		_parse();

		if( initSequence != null ) {
			programBuilder.addChild( new DefinitionNode( getContext(), "init", initSequence ) );
		}

		if( main != null ) {
			programBuilder.addChild( main );
		}

		if( !programBuilder.isJolieModuleSystem() && main != null ) {
			programBuilder.transformProgramToModuleSystem();
		} else if( hasIncludeDirective ) {
			// [backward-compatibility] for include directive, remove Deployment Instructions which was added
			// during include file parsing process
			programBuilder.removeModuleScopeDeploymentInstructions();
		}
		return programBuilder.toProgram();
	}

	private void parseLoop( ParsingRunnable... parseRunnables )
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

		if( t.isNot( Scanner.TokenType.EOF ) ) {
			// A term not parsable has been found, throw exception
			setStartLine();
			setEndLine();
			throwExceptionWithScope( "Unexpected term", null, "outer" );
		}
	}

	private void _parse()
		throws IOException, ParserException {
		parseLoop(
			this::parseImport,
			this::parseConstants,
			this::parseExecution,
			this::parseCorrelationSets,
			this::parseTypes,
			this::parseInterface,
			this::parsePort,
			this::parseEmbedded,
			this::parseService,
			this::parseCode );
	}


	private void parseTypes()
		throws IOException, ParserException {

		boolean keepRun = true;

		while( keepRun ) {
			int startLine = line(); // remember the line we started parsing type at
			Optional< Scanner.Token > forwardDocToken = parseForwardDocumentation();
			Optional< Scanner.Token > accessModifierToken = parseAccessModifier();
			if( token.isKeyword( "type" ) ) {
				AccessModifier accessModifier = AccessModifier.PUBLIC;
				if( accessModifierToken.isPresent() ) {
					accessModifier = accessModifierToken.get().is( Scanner.TokenType.PRIVATE ) ? AccessModifier.PRIVATE
						: AccessModifier.PUBLIC;
					accessModifierToken = Optional.empty();
				}

				String typeName;
				TypeDefinition currentType;
				nextToken();
				int startColumn = errorColumn(); // remember column of the start of the typename
				typeName = token.content();
				int endLine = line(); // remember the line we are ending the type at
				eat( Scanner.TokenType.ID, "expected type name" );
				// Creating a new parsingContext manually to get the correct line and column for the type
				// Used in error messages and symbolTable for the vscode extension
				ParsingContext context = new URIParsingContext( getContext().source(), startLine, endLine, startColumn,
					startColumn + typeName.length(), codeLine() );
				if( token.is( Scanner.TokenType.COLON ) ) {
					nextToken();
				} else {
					prependToken( new Scanner.Token( Scanner.TokenType.ID, NativeType.VOID.id() ) );
					nextToken();
				}
				// call parseType with the context with correct lines and columns
				currentType = parseType( typeName, accessModifier, context );
				if( forwardDocToken.isPresent() ) {
					parseBackwardAndSetDocumentation( currentType, forwardDocToken );
				} else {
					parseBackwardAndSetDocumentation( currentType, Optional.empty() );
				}

				typeName = currentType.name();

				definedTypes.put( typeName, currentType );
				programBuilder.addChild( currentType );
			} else {
				keepRun = false;
				forwardDocToken.ifPresent( this::addToken );
				accessModifierToken.ifPresent( this::addToken );
				addToken( token );
				nextToken();
			}
		}
	}

	private TypeDefinition parseType( String typeName, AccessModifier accessModifier, ParsingContext context )
		throws IOException, ParserException {
		TypeDefinition currentType;
		BasicTypeDefinition basicTypeDefinition = readBasicType();
		if( basicTypeDefinition == null ) { // It's a user-defined type
			currentType =
				new TypeDefinitionLink( context, typeName, Constants.RANGE_ONE_TO_ONE, token.content() );
			nextToken();
		} else {
			currentType =
				new TypeInlineDefinition( context, typeName, basicTypeDefinition, Constants.RANGE_ONE_TO_ONE );
			if( token.is( Scanner.TokenType.LCURLY ) ) { // We have sub-types to parse
				parseSubTypes( (TypeInlineDefinition) currentType );
			}
		}

		if( token.is( Scanner.TokenType.PARALLEL ) ) { // It's a sum (union, choice) type
			nextToken();
			// create a new context for the secondType (not sure if necessary)
			int startLine = line();
			int column = errorColumn();
			ParsingContext secondContext = new URIParsingContext( context.source(), startLine, startLine, column,
				column + token.content().length(), codeLine() );
			final TypeDefinition secondType = parseType( typeName, accessModifier, secondContext );
			return new TypeChoiceDefinition( context, typeName, Constants.RANGE_ONE_TO_ONE, currentType,
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
				setStartLine(); // set startLine for eventual error
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
							setEndLine(); // set endLine for error
							eatIdentifier( "expected type for node", "", Keywords.TYPE );
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

						if( type.hasSubType( currentSubType.name() ) ) {
							throwException( "sub-type " + currentSubType.name()
								+ " conflicts with another sub-type with the same name" );
						}
						type.putSubType( currentSubType );
					}
				}

				keepRun = false;
				// if ( haveComment ) {
				// addToken( commentToken );
				// addToken( token );
				// nextToken();
				// }
			}
		}
		eat( Scanner.TokenType.RCURLY, "RCURLY expected" );
	}

	private TypeDefinition parseSubType( String id, Range cardinality )
		throws IOException, ParserException {
		String currentTokenContent = token.content();
		TypeDefinition subType;
		// SubType id

		BasicTypeDefinition basicTypeDefinition = readBasicType();
		if( basicTypeDefinition == null ) { // It's a user-defined type
			subType = new TypeDefinitionLink( getContext(), id, cardinality, currentTokenContent );
			nextToken();
		} else {
			subType = new TypeInlineDefinition( getContext(), id, basicTypeDefinition, cardinality );

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



	/*
	 * set maxNumberOfParameters = null to unbound the list
	 */
	private ArrayList< Integer > parseListOfInteger( Integer minNumberOfParameters, Integer maxNumberOfParameters,
		String predicate ) throws IOException, ParserException {
		ArrayList< Integer > arrayList = new ArrayList<>();
		eat( Scanner.TokenType.LSQUARE, "a list of parameters is expected" );
		while( token.type() != Scanner.TokenType.RSQUARE ) {
			if( token.type() != Scanner.TokenType.INT && token.type() != Scanner.TokenType.ASTERISK ) {
				throwException( "Expected a parameter of type integer, found " + token.content() );
			}
			if( token.type() == Scanner.TokenType.INT ) {
				arrayList.add( Integer.valueOf( token.content() ) );
			} else {
				arrayList.add( Integer.MAX_VALUE );
			}
			nextToken();
			if( token.type() == Scanner.TokenType.COMMA ) {
				nextToken();
			}
		}
		eat( Scanner.TokenType.RSQUARE, "] expected" );
		if( arrayList.size() < minNumberOfParameters ) {
			throwException(
				"Expected minimum number of parameters for predicate " + predicate + ", " + minNumberOfParameters );
		}

		if( maxNumberOfParameters != null && arrayList.size() > maxNumberOfParameters ) {
			throwException(
				"Expected maximum number of parameters for predicate " + predicate + ", " + maxNumberOfParameters );
		}

		return arrayList;
	}

	/*
	 * set maxNumberOfParameters = null to unbound the list
	 */
	private ArrayList< String > parseListOfString( Integer minNumberOfParameters, Integer maxNumberOfParameters,
		String predicate ) throws IOException, ParserException {
		ArrayList< String > arrayList = new ArrayList<>();
		eat( Scanner.TokenType.LSQUARE, "a list of parameters is expected" );
		while( token.type() != Scanner.TokenType.RSQUARE ) {
			if( token.type() != Scanner.TokenType.STRING ) {
				throwException( "Expected a parameter of type string, found " + token.content() );
			}
			arrayList.add( token.content().replaceAll( "\"", "" ) );
			nextToken();
			if( token.type() == Scanner.TokenType.COMMA ) {
				nextToken();
			}
		}
		eat( Scanner.TokenType.RSQUARE, "] expected" );

		if( arrayList.size() < minNumberOfParameters ) {
			throwException(
				"Expected minimum number of parameters for predicate " + predicate + ", " + minNumberOfParameters );
		}

		if( maxNumberOfParameters != null && arrayList.size() > maxNumberOfParameters ) {
			throwException(
				"Expected maximum number of parameters for predicate " + predicate + ", " + maxNumberOfParameters );
		}

		return arrayList;
	}

	/*
	 * set maxNumberOfParameters = null to unbound the list
	 */
	private ArrayList< Double > parseListOfDouble( Integer minNumberOfParameters, Integer maxNumberOfParameters,
		String predicate ) throws IOException, ParserException {
		ArrayList< Double > arrayList = new ArrayList<>();
		eat( Scanner.TokenType.LSQUARE, "a list of parameters is expected" );
		while( token.type() != Scanner.TokenType.RSQUARE ) {
			if( token.type() != Scanner.TokenType.DOUBLE && token.type() != Scanner.TokenType.ASTERISK ) {
				throwException( "Expected a parameter of type string, found " + token.content() );
			}
			if( token.type() == Scanner.TokenType.DOUBLE ) {
				arrayList.add( Double.valueOf( token.content() ) );
			} else {
				arrayList.add( Double.MAX_VALUE );
			}
			nextToken();
			if( token.type() == Scanner.TokenType.COMMA ) {
				nextToken();
			}
		}
		eat( Scanner.TokenType.RSQUARE, "] expected" );

		if( arrayList.size() < minNumberOfParameters ) {
			throwException(
				"Expected minimum number of parameters for predicate " + predicate + ", " + minNumberOfParameters );
		}

		if( maxNumberOfParameters != null && arrayList.size() > maxNumberOfParameters ) {
			throwException(
				"Expected maximum number of parameters for predicate " + predicate + ", " + maxNumberOfParameters );
		}

		return arrayList;
	}

	/*
	 * set maxNumberOfParameters = null to unbound the list
	 */
	private ArrayList< Long > parseListOfLong( Integer minNumberOfParameters, Integer maxNumberOfParameters,
		String predicate ) throws IOException, ParserException {
		ArrayList< Long > arrayList = new ArrayList<>();
		eat( Scanner.TokenType.LSQUARE, "a list of parameters is expected" );
		while( token.type() != Scanner.TokenType.RSQUARE ) {
			if( token.type() != Scanner.TokenType.LONG && token.type() != Scanner.TokenType.ASTERISK ) {
				throwException( "Expected a parameter of type string, found " + token.content() );
			}
			if( token.type() == Scanner.TokenType.LONG ) {
				arrayList.add( Long.valueOf( token.content() ) );
			} else {
				arrayList.add( Long.MAX_VALUE );
			}
			nextToken();
			if( token.type() == Scanner.TokenType.COMMA ) {
				nextToken();
			}
		}
		eat( Scanner.TokenType.RSQUARE, "] expected" );

		if( arrayList.size() < minNumberOfParameters ) {
			throwException(
				"Expected minimum number of parameters for predicate " + predicate + ", " + minNumberOfParameters );
		}

		if( maxNumberOfParameters != null && arrayList.size() > maxNumberOfParameters ) {
			throwException(
				"Expected maximum number of parameters for predicate " + predicate + ", " + maxNumberOfParameters );
		}

		return arrayList;
	}

	private BasicTypeDefinition readBasicType() throws IOException, ParserException {
		List< BasicTypeRefinement< ? > > basicTypeRefinementList = new ArrayList<>();
		if( token.is( Scanner.TokenType.CAST_INT ) ) {
			nextToken();
			if( token.is( Scanner.TokenType.LPAREN ) ) {
				nextToken();
				while( token.type() != Scanner.TokenType.RPAREN ) {
					if( !token.type().equals( Scanner.TokenType.ID ) ) {
						throwException( "Basic type Refinement predicate expected" );
					}
					String predicate = token.content();
					nextToken();
					if( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) != null ) {
						eat( Scanner.TokenType.LPAREN, "( expected" );

						switch( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) ) {
						case RANGES:
							BasicTypeRefinementIntegerRanges basicTypeRefinementIntegerRanges =
								new BasicTypeRefinementIntegerRanges();
							while( token.type() != Scanner.TokenType.RPAREN ) {

								ArrayList< Integer > parametersInterval = parseListOfInteger( 2, 2, predicate );
								basicTypeRefinementIntegerRanges
									.addInterval( new BasicTypeRefinementIntegerRanges.Interval(
										parametersInterval.get( 0 ), parametersInterval.get( 1 ) ) );
								if( token.type() == Scanner.TokenType.COMMA ) {
									eat( Scanner.TokenType.COMMA, "" );
								} else if( token.type() != Scanner.TokenType.RPAREN ) {
									throwException( ", expected" );
								}
							}
							basicTypeRefinementList.add( basicTypeRefinementIntegerRanges );
							break;
						default:
							throwException(
								"Basic type Refinement predicate " + predicate + " not supported for int" );
						}

						eat( Scanner.TokenType.RPAREN, ") expected" );
					} else {
						StringBuilder supportedList = new StringBuilder().append( " " );
						BASIC_TYPE_REFINED_PREDICATES.keySet().stream()
							.forEach( s -> supportedList.append( s ).append( " " ) );
						throwException( "Basic type Refinement predicate not supported. Supported list ["
							+ supportedList + "], found " + predicate );
					}
				}
				eat( Scanner.TokenType.RPAREN, ") expected" );
			}
			return BasicTypeDefinition.of( NativeType.INT, basicTypeRefinementList );
		} else if( token.is( Scanner.TokenType.CAST_DOUBLE ) ) {
			nextToken();
			if( token.is( Scanner.TokenType.LPAREN ) ) {
				nextToken();
				while( token.type() != Scanner.TokenType.RPAREN ) {
					if( !token.type().equals( Scanner.TokenType.ID ) ) {
						throwException( "Basic type Refinement predicate expected" );
					}
					String predicate = token.content();
					nextToken();
					if( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) != null ) {
						eat( Scanner.TokenType.LPAREN, "( expected" );

						switch( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) ) {
						case RANGES:
							BasicTypeRefinementDoubleRanges basicTypeRefinementDoubleRanges =
								new BasicTypeRefinementDoubleRanges();
							while( token.type() != Scanner.TokenType.RPAREN ) {

								ArrayList< Double > parametersInterval = parseListOfDouble( 2, 2, predicate );
								basicTypeRefinementDoubleRanges
									.addInterval( new BasicTypeRefinementDoubleRanges.Interval(
										parametersInterval.get( 0 ), parametersInterval.get( 1 ) ) );
								if( token.type() == Scanner.TokenType.COMMA ) {
									eat( Scanner.TokenType.COMMA, "" );
								} else if( token.type() != Scanner.TokenType.RPAREN ) {
									throwException( ", expected" );
								}
							}
							basicTypeRefinementList.add( basicTypeRefinementDoubleRanges );
							break;
						default:
							throwException(
								"Basic type Refinement predicate " + predicate + " not supported for int" );
						}

						eat( Scanner.TokenType.RPAREN, ") expected" );
					} else {
						StringBuilder supportedList = new StringBuilder().append( " " );
						BASIC_TYPE_REFINED_PREDICATES.keySet().stream()
							.forEach( s -> supportedList.append( s ).append( " " ) );
						throwException( "Basic type Refinement predicate not supported. Supported list ["
							+ supportedList + "], found " + predicate );
					}
				}
				eat( Scanner.TokenType.RPAREN, ") expected" );
			}
			return BasicTypeDefinition.of( NativeType.DOUBLE, basicTypeRefinementList );
		} else if( token.is( Scanner.TokenType.CAST_STRING ) ) {
			nextToken();
			if( token.is( Scanner.TokenType.LPAREN ) ) {
				nextToken();
				while( token.type() != Scanner.TokenType.RPAREN ) {
					if( !token.type().equals( Scanner.TokenType.ID ) ) {
						throwException( "Basic type Refinement predicate expected" );
					}
					String predicate = token.content();
					nextToken();
					eat( Scanner.TokenType.LPAREN, "( expected" );
					if( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) != null ) {
						switch( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) ) {
						case LENGTH:

							ArrayList< Integer > parametersLength = parseListOfInteger( 2, 2, predicate );
							BasicTypeRefinementStringLength basicTypeRefinementStringLength =
								new BasicTypeRefinementStringLength( parametersLength.get( 0 ),
									parametersLength.get( 1 ) );
							basicTypeRefinementList.add( basicTypeRefinementStringLength );
							break;
						case ENUM:
							ArrayList< String > parametersList = parseListOfString( 1, null, predicate );
							BasicTypeRefinementStringList basicTypeRefinementStringList =
								new BasicTypeRefinementStringList( parametersList );
							basicTypeRefinementList.add( basicTypeRefinementStringList );

							break;
						case REGEX:
							assertToken( Scanner.TokenType.STRING, "Expected regex string for predicate " + predicate );
							basicTypeRefinementList.add( new BasicTypeRefinementStringRegex( token.content() ) );
							nextToken();
							break;
						default:
							throwException(
								"Basic type refinement predicate " + predicate + " not supported for string" );
						}
					} else {
						StringBuilder supportedList = new StringBuilder().append( " " );
						BASIC_TYPE_REFINED_PREDICATES.keySet().stream()
							.forEach( s -> supportedList.append( s ).append( " " ) );
						throwException( "Basic type Refinement predicate not supported. Supported list ["
							+ supportedList + "], found " + predicate );
					}
					eat( Scanner.TokenType.RPAREN, ") expected" );
				}
				eat( Scanner.TokenType.RPAREN, ") expected" );

			}
			return BasicTypeDefinition.of( NativeType.STRING, basicTypeRefinementList );
		} else if( token.is( Scanner.TokenType.CAST_LONG ) ) {
			nextToken();
			if( token.is( Scanner.TokenType.LPAREN ) ) {
				nextToken();
				while( token.type() != Scanner.TokenType.RPAREN ) {
					if( !token.type().equals( Scanner.TokenType.ID ) ) {
						throwException( "Basic type Refinement predicate expected" );
					}
					String predicate = token.content();
					nextToken();
					if( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) != null ) {
						eat( Scanner.TokenType.LPAREN, "( expected" );

						switch( BASIC_TYPE_REFINED_PREDICATES.get( predicate ) ) {
						case RANGES:
							BasicTypeRefinementLongRanges basicTypeRefinementLongRanges =
								new BasicTypeRefinementLongRanges();
							while( token.type() != Scanner.TokenType.RPAREN ) {

								ArrayList< Long > parametersInterval = parseListOfLong( 2, 2, predicate );
								basicTypeRefinementLongRanges
									.addInterval( new BasicTypeRefinementLongRanges.Interval(
										parametersInterval.get( 0 ), parametersInterval.get( 1 ) ) );
								if( token.type() == Scanner.TokenType.COMMA ) {
									eat( Scanner.TokenType.COMMA, "" );
								} else if( token.type() != Scanner.TokenType.RPAREN ) {
									throwException( ", expected" );
								}
							}
							basicTypeRefinementList.add( basicTypeRefinementLongRanges );
							break;
						default:
							throwException(
								"Basic type Refinement predicate " + predicate + " not supported for int" );
						}

						eat( Scanner.TokenType.RPAREN, ") expected" );
					} else {
						StringBuilder supportedList = new StringBuilder().append( " " );
						BASIC_TYPE_REFINED_PREDICATES.keySet().stream()
							.forEach( s -> supportedList.append( s ).append( " " ) );
						throwException( "Basic type Refinement predicate not supported. Supported list ["
							+ supportedList + "], found " + predicate );
					}
				}
				eat( Scanner.TokenType.RPAREN, ") expected" );
			}
			return BasicTypeDefinition.of( NativeType.LONG, basicTypeRefinementList );
		} else {
			NativeType nativeType = NativeType.fromString( token.content() );
			if( nativeType == null ) {
				return null;
			}
			nextToken();
			return BasicTypeDefinition.of( nativeType );
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

	private EmbedServiceNode parseEmbeddedServiceNode()
		throws IOException, ParserException {
		nextToken();
		setStartLine();
		int startLine = getContext().startLine();
		int startColumn = getContext().startColumn();
		int endColumn = -1;
		ParsingContext ctx = getContext();
		String serviceName = token.content();
		OutputPortInfo bindingPort = null;
		boolean hasNewKeyword = false;
		OLSyntaxNode passingParam = null;
		nextToken();
		if( token.is( Scanner.TokenType.LPAREN ) ) {
			nextToken();
			if( !token.is( Scanner.TokenType.RPAREN ) ) {
				passingParam = parseBasicExpression();
			}
			eat( Scanner.TokenType.RPAREN, "expected )" );
		}
		if( token.is( Scanner.TokenType.AS ) ) {
			nextToken();
			hasNewKeyword = true;
			assertToken( Scanner.TokenType.ID, "expected output port name" );
			endColumn = getContext().endColumn();
			setEndLine();
			ctx =
				new URIParsingContext( getContext().source(), startLine, endLine(), startColumn,
					endColumn, codeLine() );
			bindingPort = new OutputPortInfo( ctx, token.content() );
			nextToken();
		} else if( token.isKeyword( "in" ) ) {
			nextToken();
			assertToken( Scanner.TokenType.ID, "expected output port name" );
			endColumn = getContext().endColumn();
			setEndLine();
			ctx =
				new URIParsingContext( getContext().source(), startLine, endLine(), startColumn,
					endColumn, codeLine() );
			bindingPort = new OutputPortInfo( ctx, token.content() );
			nextToken();
		}
		return new EmbedServiceNode( ctx, serviceName, bindingPort, hasNewKeyword, passingParam );
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
		if( token.isKeyword( "cset" ) ) {
			for( CorrelationSetInfo csetInfo : _parseCorrelationSets() ) {
				programBuilder.addChild( csetInfo );
			}
		}
	}

	private CorrelationSetInfo[] _parseCorrelationSets() throws IOException, ParserException {
		List< CorrelationSetInfo > result = new ArrayList<>();

		while( token.isKeyword( "cset" ) ) {
			nextToken();
			/*
			 * assertToken( Scanner.TokenType.ID, "expected correlation set name" ); String csetName =
			 * token.content(); nextToken();
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
					TypeDefinition aliasType =
						definedTypes.getOrDefault( typeName, new TypeDefinitionLink( getContext(),
							typeName, Constants.RANGE_ONE_TO_ONE, typeName ) );
					aliases.add( new CorrelationAliasInfo( aliasType, parseVariablePath() ) );
				}
				variables.add( new CorrelationVariableInfo( correlationVariablePath, aliases ) );
				if( token.is( Scanner.TokenType.COMMA ) ) {
					nextToken();
				} else {
					break;
				}
			}

			result.add( new CorrelationSetInfo( getContext(), variables ) );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
		return result.toArray( new CorrelationSetInfo[ 0 ] );
	}

	private void parseExecution()
		throws IOException, ParserException {
		if( token.is( Scanner.TokenType.EXECUTION ) ) {
			programBuilder.addChild( _parseExecutionInfo() );
		}
	}

	private ExecutionInfo _parseExecutionInfo()
		throws IOException, ParserException {
		Constants.ExecutionMode mode = Constants.ExecutionMode.SEQUENTIAL;
		setStartLine(); // remember line we started parsing execution info
		nextToken();
		boolean inCurlyBrackets = false;
		if( token.is( Scanner.TokenType.COLON ) ) {
			nextToken();
		} else if( token.is( Scanner.TokenType.LCURLY ) ) {
			inCurlyBrackets = true;
			nextToken();
		} else {
			setEndLine(); // remember ending line of parsing execution for error
			throwException( "expected : or { after execution" );
		}
		setEndLine(); // remember ending line of parsing execution for error
		// assert token, set scope for eventual error
		assertToken( Scanner.TokenType.ID, "expected execution modality", null, Keywords.EXECUTION );
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
			// throw error with scope set
			throwExceptionWithScope( "Expected execution mode", null, Keywords.EXECUTION );
			break;
		}
		nextToken();
		if( inCurlyBrackets ) {
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
		return new ExecutionInfo( getContext(), mode );
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
			hasIncludeDirective = true;
			Scanner oldScanner = scanner();
			assertToken( Scanner.TokenType.STRING, "expected filename to include" );
			String includeStr = token.content();
			includeFile = null;

			for( int i = 0; i < includePaths.length && includeFile == null; i++ ) {
				try {
					includeFile = retrieveIncludeFile( includePaths[ i ], includeStr );
				} catch( URISyntaxException e ) {
					throwException( e.getMessage() );
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
			_parse();
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

	private void parsePort()
		throws IOException, ParserException {
		Optional< Scanner.Token > forwardDocToken = parseForwardDocumentation();
		Optional< Scanner.Token > accessModifierToken = parseAccessModifier();

		final DocumentedNode node;
		final PortInfo p;
		if( token.isKeyword( "inputPort" ) || token.isKeyword( "outputPort" ) ) {
			if( accessModifierToken.isPresent() ) {
				throwException( "unexpected access modifier before inputPort/outputPort" );
			}
			p = _parsePort();
			programBuilder.addChild( p );
			node = p;
			parseBackwardAndSetDocumentation( node, forwardDocToken );
		} else {
			forwardDocToken.ifPresent( this::addToken );
			accessModifierToken.ifPresent( this::addToken );
			addToken( token );
			nextToken();
		}
	}

	private PortInfo _parsePort()
		throws IOException, ParserException {
		PortInfo portInfo = null;
		if( token.isKeyword( "inputPort" ) ) {
			portInfo = parseInputPortInfo();
		} else if( token.isKeyword( "outputPort" ) ) {
			portInfo = parseOutputPortInfo();
		}
		return portInfo;
	}

	private Optional< Scanner.Token > parseAccessModifier()
		throws IOException, ParserException {
		Scanner.Token accessModToken = null;
		if( token.is( Scanner.TokenType.PRIVATE ) || token.is( Scanner.TokenType.PUBLIC ) ) {
			accessModToken = token;
			nextToken();
		}
		return Optional.ofNullable( accessModToken );
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
			// Optional< Scanner.Token > backwardDoc = parseBackwardDocumentation();
			// if( backwardDoc.isPresent() ) {
			// node.setDocumentation( backwardDoc.get().content() );
			// } else {
			// String forwardDoc = forwardDocToken
			// .orElse( new Scanner.Token( Scanner.TokenType.DOCUMENTATION_FORWARD, "" ) ).content();
			// node.setDocumentation( forwardDoc );
			// }
			// </Java 8>

			parseBackwardDocumentation().ifPresentOrElse( doc -> node.setDocumentation( doc.content() ),
				() -> node.setDocumentation(
					(forwardDocToken.orElse( new Scanner.Token( Scanner.TokenType.DOCUMENTATION_FORWARD, "" ) ))
						.content() ) );
		} else {
			forwardDocToken.ifPresent( this::addToken );
			addToken( token );
			nextToken();
		}
	}

	private void parseInterface()
		throws IOException, ParserException {
		Optional< Scanner.Token > forwardDocToken = parseForwardDocumentation();
		Optional< Scanner.Token > accessModifierToken = parseAccessModifier();
		if( token.isKeyword( "interface" ) ) {
			nextToken();
			DocumentedNode docNode = null;
			AccessModifier accessModifier =
				(accessModifierToken.isPresent() && accessModifierToken.get().is( Scanner.TokenType.PRIVATE ))
					? AccessModifier.PRIVATE
					: AccessModifier.PUBLIC;

			final InterfaceDefinition iface;
			if( token.isKeyword( "extender" ) ) {
				nextToken();
				iface = _parseInterfaceExtender( accessModifier );
				docNode = iface;
			} else {
				iface = _parseInterface( accessModifier );
				docNode = iface;
			}
			if( docNode != null ) {
				parseBackwardAndSetDocumentation( docNode, forwardDocToken );
			}
			programBuilder.addChild( iface );
		} else {
			forwardDocToken.ifPresent( this::addToken );
			accessModifierToken.ifPresent( this::addToken );
			addToken( token );
			nextToken();
		}
	}

	private OutputPortInfo createInternalServicePort( String name, InterfaceDefinition[] interfaceList )
		throws ParserException {
		OutputPortInfo p = new OutputPortInfo( getContext(), name );

		for( InterfaceDefinition interfaceDefinition : interfaceList ) {
			p.addInterface( interfaceDefinition );
		}
		return p;
	}

	private InputPortInfo createInternalServiceInputPort( String serviceName, InterfaceDefinition[] interfaceList )
		throws ParserException {
		InputPortInfo iport = new InputPortInfo(
			getContext(),
			serviceName + "InputPort", // input port name
			new ConstantStringExpression( getContext(), Constants.LOCAL_LOCATION_KEYWORD ),
			null,
			new InputPortInfo.AggregationItemInfo[] {},
			Collections.< String, String >emptyMap() );
		for( InterfaceDefinition i : interfaceList ) {
			iport.addInterface( i );
		}
		return iport;
	}

	/**
	 * Parses an internal service, i.e. service service_name {}
	 */
	private EmbeddedServiceNode createInternalService(
		ParsingContext ctx,
		String serviceName,
		InterfaceDefinition[] ifaces,
		SequenceStatement init,
		DefinitionNode main,
		ProgramBuilder parentProgramBuilder )
		throws IOException, ParserException {
		// add output port to main program
		parentProgramBuilder.addChild( createInternalServicePort( serviceName, ifaces ) );

		// create Program representing the internal service
		ProgramBuilder internalServiceProgramBuilder = new ProgramBuilder( ctx );

		// copy children of parent to embedded service
		for( OLSyntaxNode child : parentProgramBuilder.children() ) {
			if( child instanceof OutputPortInfo
				|| child instanceof TypeDefinition ) {
				internalServiceProgramBuilder.addChild( child );
			}
		}

		// set execution to always concurrent
		internalServiceProgramBuilder.addChild( new ExecutionInfo( getContext(), Constants.ExecutionMode.CONCURRENT ) );

		// add input port to internal service
		internalServiceProgramBuilder.addChild( createInternalServiceInputPort( serviceName, ifaces ) );

		// add init if defined in internal service
		if( init != null ) {
			internalServiceProgramBuilder.addChild( new DefinitionNode( getContext(), "init", init ) );
		}

		// add main defined in internal service
		internalServiceProgramBuilder.addChild( main );

		// create internal embedded service node
		EmbeddedServiceNode internalServiceNode =
			new EmbeddedServiceNode( getContext(), Constants.EmbeddedServiceType.INTERNAL, serviceName, serviceName );

		internalServiceProgramBuilder.transformProgramToModuleSystem();

		// add internal service program to embedded service node
		internalServiceNode.setProgram( internalServiceProgramBuilder.toProgram() );
		return internalServiceNode;
	}

	private InterfaceDefinition[] parseInternalServiceInterface()
		throws IOException, ParserException {
		nextToken();
		eat( Scanner.TokenType.COLON, "expected : after Interfaces" );
		boolean keepRun = true;
		List< InterfaceDefinition > currInternalServiceIfaceList = new ArrayList<>();

		while( keepRun ) {
			assertToken( Scanner.TokenType.ID, "expected interface name" );
			InterfaceDefinition i = new InterfaceDefinition( getContext(), token.content() );
			currInternalServiceIfaceList.add( i );
			nextToken();

			if( token.is( Scanner.TokenType.COMMA ) ) {
				nextToken();
			} else {
				keepRun = false;
			}
		}
		return currInternalServiceIfaceList.toArray( new InterfaceDefinition[ 0 ] );
	}

	private Pair< String, TypeDefinition > parseServiceParameter()
		throws IOException, ParserException {
		if( token.is( Scanner.TokenType.LPAREN ) ) {
			nextToken();
			if( token.is( Scanner.TokenType.RPAREN ) ) { // case ( )
				nextToken();
				return null;
			} else { // case ( path: type )
				assertToken( Scanner.TokenType.ID, "expected parameter variable name" );
				String paramPath = token.content();
				nextToken();

				eat( Scanner.TokenType.COLON, "expected :" );
				String typeName = token.content();
				TypeDefinition parameterType = parseType( typeName, AccessModifier.PRIVATE, getContext() );

				eat( Scanner.TokenType.RPAREN, "expected )" );
				return new Pair<>( paramPath, parameterType );
			}
		} else {
			return null;
		}
	}

	private ServiceNode createForeignServiceNode(
		ParsingContext ctx,
		String serviceName,
		Pair< String, TypeDefinition > parameter,
		AccessModifier accessModifier,
		ProgramBuilder serviceBlockProgramBuilder,
		EmbeddedServiceType technology,
		Map< String, String > implementationConfiguration ) {
		return ServiceNode.create( ctx, serviceName, accessModifier, serviceBlockProgramBuilder.toProgram(),
			parameter, technology, implementationConfiguration );
	}

	private ServiceNode createJolieServiceNode(
		ParsingContext ctx,
		String serviceName,
		Pair< String, TypeDefinition > parameter,
		AccessModifier accessModifier,
		SequenceStatement init,
		DefinitionNode main,
		ProgramBuilder parentProgramBuilder,
		ProgramBuilder serviceBlockProgramBuilder ) {

		ProgramBuilder serviceNodeProgramBuilder = new ProgramBuilder( ctx );

		// [backward-compatibility] inject top-level deployment instructions to the service node
		// so the include directive is still working in Module System, These deployment instructions of
		// parent's programBuilder are meant to remove at the end of parsing step.
		for( OLSyntaxNode child : parentProgramBuilder.children() ) {
			if( child instanceof OutputPortInfo || child instanceof InputPortInfo
				|| child instanceof EmbeddedServiceNode ) {
				serviceNodeProgramBuilder.addChild( child );
			}
		}
		// copy children of parent to embedded service
		for( OLSyntaxNode child : serviceBlockProgramBuilder.children() ) {
			serviceNodeProgramBuilder.addChild( child );
		}

		// add init if defined in internal service
		if( init != null ) {
			serviceNodeProgramBuilder.addChild( new DefinitionNode( getContext(), "init", init ) );
		}

		// add main defined in service
		if( main != null ) {
			serviceNodeProgramBuilder.addChild( main );
		}

		ServiceNode node = ServiceNode.create( ctx, serviceName, accessModifier, serviceNodeProgramBuilder.toProgram(),
			parameter );
		return node;
	}

	/**
	 * Parses a service node, i.e. service service_name ( varpath : type ) {}
	 */
	private void parseService()
		throws IOException, ParserException {
		Optional< Scanner.Token > forwardDocToken = parseForwardDocumentation();
		Optional< Scanner.Token > accessModifierToken = parseAccessModifier();
		// only proceed if a service declaration
		if( !token.isKeyword( "service" ) ) {
			forwardDocToken.ifPresent( this::addToken );
			accessModifierToken.ifPresent( this::addToken );
			addToken( token );
			nextToken();
			return;
		}
		setStartLine(); // remember line we started parsing service at
		nextToken();

		Constants.EmbeddedServiceType tech = Constants.EmbeddedServiceType.SERVICENODE;
		Map< String, String > configMap = new HashMap<>();

		setEndLine(); // remember endline of parsing service for error
		assertToken( Scanner.TokenType.ID, "expected service name" );
		ParsingContext ctx = getContext();
		String serviceName = token.content();
		nextToken();

		Pair< String, TypeDefinition > parameter = parseServiceParameter();

		setEndLine();// remember endline of parsing service for error
		// look for curly bracket with scope and scopeName in case of error
		eat( Scanner.TokenType.LCURLY, "expected {", serviceName, Keywords.SERVICE );
		// jolie internal service's Interface
		InterfaceDefinition[] internalIfaces = null;

		DefinitionNode internalMain = null;
		SequenceStatement internalInit = null;

		ProgramBuilder serviceBlockProgramBuilder = new ProgramBuilder( getContext() );
		boolean keepRun = true;

		while( keepRun ) {
			Optional< Scanner.Token > internalForwardDocToken = parseForwardDocumentation();
			switch( token.content() ) {
			case "Interfaces": // internal service node syntax
				internalIfaces = parseInternalServiceInterface();
				break;
			case "include":
				parseInclude();
				break;
			case "cset":
				for( CorrelationSetInfo csetInfo : _parseCorrelationSets() ) {
					serviceBlockProgramBuilder.addChild( csetInfo );
				}
				break;
			case "execution":
				serviceBlockProgramBuilder.addChild( _parseExecutionInfo() );
				break;
			case "courier":
				serviceBlockProgramBuilder.addChild( parseCourierDefinition() );
				break;
			case "init":
				if( internalInit == null ) {
					internalInit = new SequenceStatement( getContext() );
				}
				internalInit.addChild( parseInit() );
				break;
			case "main":
				// remember start and end line for error
				setStartLine();
				setEndLine();
				if( internalMain != null ) {
					throwException( "you must specify only one main definition" );
				}
				internalMain = parseMain();
				break;
			case "inputPort":
			case "outputPort":
				PortInfo p = _parsePort();
				parseBackwardAndSetDocumentation( ((DocumentedNode) p), internalForwardDocToken );
				serviceBlockProgramBuilder.addChild( p );
				break;
			case "define":
				serviceBlockProgramBuilder.addChild( parseDefinition() );
				break;
			case "embed":
				EmbedServiceNode embedServiceNode = parseEmbeddedServiceNode();
				if( embedServiceNode.isNewPort() ) {
					serviceBlockProgramBuilder
						.addChild( embedServiceNode.bindingPort() );
				}
				serviceBlockProgramBuilder.addChild( embedServiceNode );
				break;
			case "foreign":
				nextToken();
				String technology = token.content();
				if( technology.equals( "java" ) ) {
					tech = Constants.EmbeddedServiceType.SERVICENODE_JAVA;
				}
				nextToken();
				eat( Scanner.TokenType.LCURLY, "expected {" );
				while( token.isNot( Scanner.TokenType.RCURLY ) ) {
					String key = token.content();
					nextToken();
					eat( Scanner.TokenType.COLON, "expected :" );
					String value = "";
					while( !hasMetNewline() ) {
						if( token.is( Scanner.TokenType.DOT ) ) {
							value += ".";
						} else {
							value += token.content();
						}
						nextToken();
					}
					configMap.put( key, value );
				}
				setEndLine(); // remember end line for error
				eat( Scanner.TokenType.RCURLY, "expected }" );
			default:
				setEndLine(); // remember end line for error
				if( token.isNot( Scanner.TokenType.EOF ) ) {
					setStartLine(); // remember start line for error
				}
				// assert with scopeName and scope in case of error
				assertToken( Scanner.TokenType.RCURLY, "unexpected term found inside service " + serviceName,
					serviceName, "service" );
				keepRun = false;
			}
		}
		setEndLine(); // remember end line for error
		eat( Scanner.TokenType.RCURLY, "expected }" );
		// it is a Jolie internal service
		if( internalIfaces != null && internalIfaces.length > 0 ) {
			if( internalMain == null ) {
				setEndLine(); // remember end line for error
				throwException( "You must specify a main for service " + serviceName );
			}
			EmbeddedServiceNode node = createInternalService( ctx, serviceName, internalIfaces,
				internalInit, internalMain, programBuilder );
			programBuilder.addChild( node );
		} else {
			AccessModifier accessModifier =
				(accessModifierToken.isPresent() && accessModifierToken.get().is( Scanner.TokenType.PRIVATE ))
					? AccessModifier.PRIVATE
					: AccessModifier.PUBLIC;

			ServiceNode serviceNode = null;
			switch( tech ) {
			case SERVICENODE:
				serviceNode = createJolieServiceNode(
					ctx,
					serviceName,
					parameter,
					accessModifier,
					internalInit,
					internalMain,
					programBuilder,
					serviceBlockProgramBuilder );
				break;
			default:
				serviceNode = createForeignServiceNode(
					ctx,
					serviceName,
					parameter,
					accessModifier,
					serviceBlockProgramBuilder, tech, configMap );
			}
			programBuilder.addChild( serviceNode );
		}
	}

	private InputPortInfo parseInputPortInfo()
		throws IOException, ParserException {
		String inputPortName;
		OLSyntaxNode protocol = null;
		OLSyntaxNode location = null;
		List< InterfaceDefinition > interfaceList = new ArrayList<>();
		setStartLine(); // remember start line for error
		nextToken();
		setEndLine(); // remember end line for error
		assertToken( Scanner.TokenType.ID, "expected inputPort name", null, Keywords.INPUT_PORT );
		inputPortName = token.content();
		int startLine = getContext().startLine();
		int startColumn = getContext().startColumn();
		nextToken();
		setEndLine(); // remember end line for error
		eat( Scanner.TokenType.LCURLY, "expected {", inputPortName, Keywords.INPUT_PORT );
		InterfaceDefinition iface = new InterfaceDefinition( getContext(), "Internal interface for: " + inputPortName );

		Map< String, String > redirectionMap = new HashMap<>();
		List< InputPortInfo.AggregationItemInfo > aggregationList = new ArrayList<>();
		boolean isLocationLocal = false;

		while( token.isNot( Scanner.TokenType.RCURLY ) ) {
			if( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( iface );
			} else if( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( iface );
			} else if( token.isKeyword( "location" ) || token.isKeyword( "Location" ) ) {
				if( location != null ) {
					throwException( "Location already defined for service " + inputPortName );
				}
				nextToken();
				setEndLine(); // remember end line for error
				eat( Scanner.TokenType.COLON, "expected : after Location", inputPortName, Keywords.INPUT_PORT );
				checkConstant();
				if( token.content().startsWith( "local" ) ) {
					// check if the inputPort is listening to local protocol
					isLocationLocal = true;
				}
				location = parseBasicExpression();
			} else if( token.isKeyword( "interfaces" ) || token.isKeyword( "Interfaces" ) ) {
				nextToken();
				setEndLine(); // remember end line for error
				eat( Scanner.TokenType.COLON, "expected : after Interfaces", inputPortName, Keywords.INPUT_PORT );
				boolean keepRun = true;
				while( keepRun ) {
					setEndLine(); // remember end line for error
					assertToken( Scanner.TokenType.ID, "expected interface name", inputPortName, Keywords.INPUT_PORT );
					InterfaceDefinition i =
						new InterfaceDefinition( getContext(), token.content() );
					interfaceList.add( i );
					nextToken();

					if( token.is( Scanner.TokenType.COMMA ) ) {
						nextToken();
					} else {
						keepRun = false;
					}
				}
			} else if( token.isKeyword( "protocol" ) || token.isKeyword( "Protocol" ) ) {
				if( protocol != null ) {
					throwException( "Protocol already defined for inputPort " + inputPortName );
				}
				nextToken();
				setEndLine(); // remember end line for error
				eat( Scanner.TokenType.COLON, "expected : after Protocol", inputPortName, Keywords.INPUT_PORT );
				checkConstant();
				protocol = parseBasicExpression();
			} else if( token.isKeyword( "redirects" ) || token.isKeyword( "Redirects" ) ) {
				nextToken();
				setEndLine(); // remember end line for error
				eat( Scanner.TokenType.COLON, "expected : after Redirects", inputPortName, Keywords.INPUT_PORT );
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
				setEndLine(); // remember end line for error
				eat( Scanner.TokenType.COLON, "expected : after Aggregates", inputPortName, Keywords.INPUT_PORT );
				parseAggregationList( aggregationList );
			} else {
				setEndLine(); // remember end line for error
				throwExceptionWithScope( "Unrecognized term in inputPort " + inputPortName, inputPortName,
					Keywords.INPUT_PORT );
			}
		}
		// setting the endline before asserting (though the assert will always be true here),
		// so we have the correct last line number of code for the error
		setEndLine();
		assertToken( Scanner.TokenType.RCURLY, "} expected" );
		if( location == null ) {
			throwExceptionWithScope( "expected location URI for " + inputPortName, inputPortName,
				Keywords.INPUT_PORT );
		} else if( (interfaceList.isEmpty() && iface.operationsMap().isEmpty()) && redirectionMap.isEmpty()
			&& aggregationList.isEmpty() ) {
			throwExceptionWithScope(
				"expected at least one operation, interface, aggregation or redirection for inputPort "
					+ inputPortName,
				inputPortName, Keywords.INPUT_PORT );
		} else if( protocol == null && !isLocationLocal ) {
			throwExceptionWithScope( "expected protocol for inputPort " + inputPortName, inputPortName,
				Keywords.INPUT_PORT );
		}
		InputPortInfo iport =
			new InputPortInfo(
				new URIParsingContext( getContext().source(), startLine, endLine(), startColumn,
					getContext().endColumn(),
					codeLine() ),
				inputPortName, location, protocol,
				aggregationList.toArray( new InputPortInfo.AggregationItemInfo[ aggregationList.size() ] ),
				redirectionMap );
		nextToken();
		for( InterfaceDefinition i : interfaceList ) {
			iport.addInterface( i );
		}
		iface.copyTo( iport );
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

	private InterfaceDefinition _parseInterfaceExtender( AccessModifier accessModifier )
		throws IOException, ParserException {
		String name;
		assertToken( Scanner.TokenType.ID, "expected interface extender name" );
		name = token.content();
		nextToken();
		eat( Scanner.TokenType.LCURLY, "expected {" );
		InterfaceExtenderDefinition extender = currInterfaceExtender =
			new InterfaceExtenderDefinition( getContext(), name, accessModifier );
		parseOperations( currInterfaceExtender );
		interfaceExtenders.put( name, extender );
		eat( Scanner.TokenType.RCURLY, "expected }" );
		currInterfaceExtender = null;
		return extender;
	}

	private InterfaceDefinition _parseInterface( AccessModifier accessModifier )
		throws IOException, ParserException {
		String name;
		InterfaceDefinition iface;
		// set start and end line in case of error
		setStartLine();
		setEndLine();
		assertToken( Scanner.TokenType.ID, "expected interface name" );
		name = token.content();
		nextToken();
		setEndLine(); // remember end line for error
		eat( Scanner.TokenType.LCURLY, "expected {", name, Keywords.INTERFACE );
		iface = new InterfaceDefinition( getContext(), name, accessModifier );
		parseOperations( iface );
		setEndLine(); // remember end line for error
		eat( Scanner.TokenType.RCURLY, "expected }", name, Keywords.INTERFACE );

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

	private OutputPortInfo parseOutputPortInfo()
		throws IOException, ParserException {
		String outputPortName;
		OLSyntaxNode protocol = null;
		OLSyntaxNode location = null;
		List< InterfaceDefinition > interfaceList = new ArrayList<>();
		nextToken();
		setStartLine();
		assertToken( Scanner.TokenType.ID, "expected output port identifier" );
		outputPortName = token.content();
		int startLine = getContext().startLine();
		int startColumn = getContext().startColumn();
		nextToken();
		eat( Scanner.TokenType.LCURLY, "expected {" );

		InterfaceDefinition iface =
			new InterfaceDefinition( getContext(), "Internal interface for: " + outputPortName );

		boolean keepRun = true;
		while( keepRun ) {
			if( token.is( Scanner.TokenType.OP_OW ) ) {
				parseOneWayOperations( iface );
			} else if( token.is( Scanner.TokenType.OP_RR ) ) {
				parseRequestResponseOperations( iface );
			} else if( token.isKeyword( "interfaces" ) || token.isKeyword( "Interfaces" ) ) {
				nextToken();
				// set start and end line in case of error
				setStartLine();
				setEndLine();
				eat( Scanner.TokenType.COLON, "expected : after Interfaces", outputPortName, Keywords.OUTPUT_PORT );
				boolean r = true;
				while( r ) {
					assertToken( Scanner.TokenType.ID, "expected interface name" );
					InterfaceDefinition i = new InterfaceDefinition( getContext(), token.content() );
					interfaceList.add( i );
					nextToken();

					if( token.is( Scanner.TokenType.COMMA ) ) {
						nextToken();
					} else {
						r = false;
					}
				}
			} else if( token.isKeyword( "location" ) || token.isKeyword( "Location" ) ) {
				if( location != null ) {
					throwException( "Location already defined for output port " + outputPortName );
				}

				nextToken();
				// set start and end line in case of error
				setStartLine();
				setEndLine();
				eat( Scanner.TokenType.COLON, "expected : after Location", outputPortName, Keywords.OUTPUT_PORT );
				checkConstant();
				location = parseBasicExpression();
			} else if( token.isKeyword( "protocol" ) || token.isKeyword( "Protocol" ) ) {
				if( protocol != null ) {
					throwException( "Protocol already defined for output port " + outputPortName );
				}

				nextToken();
				// set start and end line in case of error
				setStartLine();
				setEndLine();
				eat( Scanner.TokenType.COLON, "expected : after Protocol", outputPortName, Keywords.OUTPUT_PORT );
				checkConstant();
				protocol = parseBasicExpression();
			} else {
				keepRun = false;
			}

		}
		setEndLine();
		OutputPortInfo p =
			new OutputPortInfo( new URIParsingContext( getContext().source(), startLine, endLine(), startColumn,
				getContext().endColumn(),
				codeLine() ), outputPortName );
		p.setProtocol( protocol );
		p.setLocation( location );
		for( InterfaceDefinition i : interfaceList )
			p.addInterface( i );
		iface.copyTo( p );
		eat( Scanner.TokenType.RCURLY, "expected }" );
		return p;
	}

	private void parseOneWayOperations( OperationCollector oc )
		throws IOException, ParserException {
		nextToken();
		eat( Scanner.TokenType.COLON, "expected : after OneWay expression" );

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
					String typeName = token.content();
					TypeDefinition type = definedTypes.getOrDefault( typeName, new TypeDefinitionLink( getContext(),
						typeName, Constants.RANGE_ONE_TO_ONE, typeName ) );
					opDecl.setRequestType( type );
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
		eat( Scanner.TokenType.COLON, "expected : after RequestResponse expression" );
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
							nextToken();
							eat( Scanner.TokenType.RPAREN, "expected )" );
						}
						TypeDefinition faultType =
							new TypeDefinitionLink( getContext(), faultIdCounter++ + "#" + faultTypeName,
								Constants.RANGE_ONE_TO_ONE, faultTypeName );
						if( definedTypes.containsKey( faultTypeName ) ) {
							faultType = definedTypes.get( faultTypeName );
						} else {
							definedTypes.put( faultTypeName, faultType );
						}
						faultTypesMap.put( faultName, faultType );
					}
				}

				TypeDefinition requestType = definedTypes.getOrDefault( requestTypeName,
					new TypeDefinitionLink( getContext(), requestTypeName,
						Constants.RANGE_ONE_TO_ONE, requestTypeName ) );

				TypeDefinition responseType = definedTypes.getOrDefault( responseTypeName,
					new TypeDefinitionLink( getContext(), responseTypeName,
						Constants.RANGE_ONE_TO_ONE, responseTypeName ) );

				RequestResponseOperationDeclaration opRR =
					new RequestResponseOperationDeclaration(
						getContext(),
						opId,
						requestType,
						responseType,
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
		eat( Scanner.TokenType.LCURLY, "expected { after procedure identifier", "", Keywords.MAIN );
		DefinitionNode retVal = new DefinitionNode( getContext(), "main", parseProcess() );
		eat( Scanner.TokenType.RCURLY, "expected } after procedure definition", "", Keywords.MAIN );
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
				// nextToken();
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
		setStartLine(); // set start line in case of error

		switch( token.type() ) {
		case LSQUARE:
			retVal = parseNDChoiceStatement();
			break;
		case PROVIDE:
			nextToken();
			retVal = parseProvideUntilStatement();
			break;
		case ID:
			if( checkConstant() ) {
				throwException( "cannot modify a constant in a statement" );
			}
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
			final String faultName;
			final Optional< OLSyntaxNode > expression;
			if( token.is( Scanner.TokenType.LPAREN ) ) {
				nextToken();
				checkConstant();
				assertToken(
					Scanner.TokenType.ID, "expected fault identifier" );
				faultName = token.content();
				nextToken();
				if( token.is( Scanner.TokenType.RPAREN ) ) {
					expression = Optional.empty();
					nextToken();
				} else {
					eat( Scanner.TokenType.COMMA, "expected , or )" );
					expression = Optional.of( parseExpression() );
					eat( Scanner.TokenType.RPAREN, "expected )" );
				}
			} else if( token.isIdentifier() ) {
				checkConstant();
				faultName = token.content();
				nextToken();
				expression =
					token.is( Scanner.TokenType.LPAREN ) ? Optional.of( inParens( this::parseExpression ) )
						: Optional.empty();
			} else {
				faultName = null;
				expression = null;
				throwException( "expected fault name after throw, for example throw MyFault" );
			}
			retVal = expression.isPresent() ? new ThrowStatement( getContext(), faultName, expression.get() )
				: new ThrowStatement( getContext(), faultName );
			break;
		case INSTALL:
			nextToken();
			eat(
				Scanner.TokenType.LPAREN, "expected (" );
			Optional< InstallFunctionNode > optInstallNode = parseInstallFunction( false );
			if( !optInstallNode.isPresent() ) {
				throwException( "expected install body" );
			}
			retVal =
				new InstallStatement( getContext(), optInstallNode.get() );
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
			setEndLine(); // set end line for error
			throwExceptionWithScope( "expected basic statement", null, Keywords.MAIN );
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

	@SuppressWarnings( "unchecked" )
	private Optional< InstallFunctionNode > parseInstallFunction( boolean hadNewLine )
		throws IOException, ParserException {
		/*
		 * Check that we're not consuming an input choice first. (This can happen for sequences separated by
		 * spaces instead of ;.)
		 */
		if( token.is( Scanner.TokenType.ID ) ) {
			Scanner.Token idToken = token;
			nextToken();
			if( token.is( Scanner.TokenType.LPAREN ) ) {
				// It's an input choice
				if( hadNewLine ) {
					addToken( new Scanner.Token( Scanner.TokenType.NEWLINE ) );
				}
				addToken( new Scanner.Token( Scanner.TokenType.LSQUARE ) );
				addToken( idToken );
				addToken( token );
				nextToken();
				return Optional.empty();
			} else {
				addToken( idToken );
				addToken( token );
				nextToken();
			}
		}

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

		return Optional.of( new InstallFunctionNode( vec.< Pair< String, OLSyntaxNode > >toArray( new Pair[ 0 ] ) ) );
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
		assertIdentifier( "Expected variable path" );
		String varId = token.content();
		nextToken();
		return _parseVariablePath( varId );
	}

	private VariablePathNode _parseVariablePath( String varId ) throws ParserException, IOException {
		return _parseVariablePathWithCtx( varId, getContext() );
	}

	private VariablePathNode _parseVariablePathWithCtx( String varId, ParsingContext ctx )
		throws IOException, ParserException {
		OLSyntaxNode expr;
		VariablePathNode path;

		switch( varId ) {
		case Keywords.GLOBAL:
			path = new VariablePathNode( ctx, Type.GLOBAL );
			break;
		case Keywords.CSETS:
			path = new VariablePathNode( ctx, Type.CSET );
			path.append( new Pair<>( new ConstantStringExpression( getContext(), varId ),
				new ConstantIntegerExpression( getContext(), 0 ) ) );
			break;
		default:
			path = new VariablePathNode( ctx, Type.NORMAL );
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
				setEndLine();
				nodeExpr = new ConstantStringExpression(
					new URIParsingContext( ctx.source(), ctx.startLine(), getContext().endLine(),
						ctx.startColumn(), getContext().endColumn(), codeLine() ),
					token.content() );
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
				iface = new InterfaceDefinition( getContext(), token.content() );
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

			if( token.is( Scanner.TokenType.LCURLY ) ) {
				nextToken();
				body = parseProcess();
				eat( Scanner.TokenType.RCURLY, "expected }" );
				eat( Scanner.TokenType.RSQUARE, "expected ]" );
			} else if( token.is( Scanner.TokenType.RSQUARE ) ) {
				nextToken();
				eat( Scanner.TokenType.LCURLY, "expected {" );
				body = parseProcess();
				eat( Scanner.TokenType.RCURLY, "expected }" );
			} else {
				body = null;
				throwException( "expected {" );
			}

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
		setStartLine(); // set start line for eventual error
		ParsingContext context = getContext();
		String outputPortId = token.content();
		nextToken();

		OLSyntaxNode outputExpression = parseOperationExpressionParameter();

		OLSyntaxNode stm;

		if( token.is( Scanner.TokenType.LPAREN ) ) { // Solicit Response operation
			VariablePathNode inputVarPath = parseOperationVariablePathParameter();
			Optional< InstallFunctionNode > function = Optional.empty();
			if( token.is( Scanner.TokenType.LSQUARE ) ) {
				boolean newLine = hasMetNewline();
				eat( Scanner.TokenType.LSQUARE, "expected [" );
				function =
					parseInstallFunction( newLine );
				if( function.isPresent() ) {
					eat(
						Scanner.TokenType.RSQUARE, "expected ]" );
				}
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

	private OLSyntaxNode parseOutputExpressionNode( String id ) throws IOException, ParserException {
		ParsingContext context = getContext();
		String outputPortId = token.content();
		nextToken();
		OLSyntaxNode outputExpression = parseOperationExpressionParameter();
		OLSyntaxNode expr = new SolicitResponseExpressionNode( context, id, outputPortId, outputExpression );

		return expr;
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

	// private NativeType readNativeType() {
	// if( token.is( Scanner.TokenType.CAST_INT ) ) {
	// return NativeType.INT;
	// } else if( token.is( Scanner.TokenType.CAST_DOUBLE ) ) {
	// return NativeType.DOUBLE;
	// } else if( token.is( Scanner.TokenType.CAST_STRING ) ) {
	// return NativeType.STRING;
	// } else if( token.is( Scanner.TokenType.CAST_LONG ) ) {
	// return NativeType.LONG;
	// } else if( token.is( Scanner.TokenType.CAST_BOOL ) ) {
	// return NativeType.BOOL;
	// } else {
	// return NativeType.fromString( token.content() );
	// }
	// }

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

			// NativeType nativeType = readNativeType();
			// if( nativeType == null ) { // It's a user-defined type
			// assertToken( Scanner.TokenType.ID, "expected type name after instanceof" );
			// }

			// String typeName = token.content();
			// TypeDefinition type = definedTypes.getOrDefault( typeName, new TypeDefinitionLink( getContext(),
			// typeName, Constants.RANGE_ONE_TO_ONE, typeName ) );

			ParsingContext context = getContext();
			// TODO: parseType should be refactored to return unnamed TypeExpressions
			ret =
				new InstanceOfExpressionNode( context, expr1,
					parseType( "#InlineType", AccessModifier.PRIVATE, context ) );
			// nextToken();
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

	private OLSyntaxNode inParens( ParsingSupplier supplier )
		throws IOException, ParserException {
		eat( Scanner.TokenType.LPAREN, "expected (" );
		OLSyntaxNode retVal = supplier.parse();
		eat( Scanner.TokenType.RPAREN, "expected )" );
		return retVal;
	}

	private OLSyntaxNode parseIfExpression()
		throws IOException, ParserException {
		ParsingContext ctx = getContext();
		eat( Scanner.TokenType.IF, "expected if" );
		OLSyntaxNode guard = inParens( () -> parseExpression() );
		OLSyntaxNode thenBranch = parseExpression();
		eat( Scanner.TokenType.ELSE, "expected else part of if-expression" );
		OLSyntaxNode elseBranch = parseExpression();
		return new IfExpressionNode( ctx, guard, thenBranch, elseBranch );
	}

	private OLSyntaxNode parseFactor()
		throws IOException, ParserException {
		OLSyntaxNode retVal = null;
		VariablePathNode path = null;

		checkConstant();
		setStartLine();
		ParsingContext ctx = getContext();

		switch( token.type() ) {
		case ID:
			String id = token.content();
			nextToken();
			if( token.type() == Scanner.TokenType.AT ) {
				nextToken();
				return parseOutputExpressionNode( id );
			} else {
				path = _parseVariablePathWithCtx( id, ctx );
				VariablePathNode freshValuePath = new VariablePathNode( getContext(), Type.NORMAL );
				freshValuePath.append( new Pair<>( new ConstantStringExpression( getContext(), "new" ),
					new ConstantIntegerExpression( getContext(), 0 ) ) );
				if( path.isEquivalentTo( freshValuePath ) ) {
					retVal = new FreshValueExpressionNode( path.context() );
					return retVal;
				}
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
			case ID:
				retVal = new VariableExpressionNode( ctx, path );
				break;
			default:
				retVal = new VariableExpressionNode( ctx, path );
				break;
			}
		} else {
			switch( token.type() ) {
			case NOT:
				nextToken();
				retVal = new NotExpressionNode( getContext(), parseFactor() );
				break;
			case STRING:
				ParsingContext ctx2 = new URIParsingContext( getContext().source(), getContext().startLine(),
					getContext().endLine(), getContext().startColumn() - 2, getContext().endColumn(), codeLine() );
				retVal = new ConstantStringExpression( ctx2, token.content() );
				nextToken();
				break;
			case INT:
				int intVal = 0;
				try {
					intVal = Integer.parseInt( token.content() );
				} catch( NumberFormatException e ) {
					throwException( e.getMessage() );
				}
				retVal = new ConstantIntegerExpression( getContext(), intVal );
				nextToken();
				break;
			case LONG:
				long longVal = 0;
				try {
					longVal = Long.parseLong( token.content() );
				} catch( NumberFormatException e ) {
					throwException( e.getMessage() );
				}
				retVal = new ConstantLongExpression( getContext(), longVal );
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
				double doubleVal = 0;
				try {
					doubleVal = Double.parseDouble( token.content() );
				} catch( NumberFormatException e ) {
					throwException( e.getMessage() );
				}
				retVal = new ConstantDoubleExpression( getContext(), doubleVal );
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
			case IF:
				retVal = parseIfExpression();
				break;
			default:
				break;
			}
		}

		if( retVal == null ) {
			if( token.is( Scanner.TokenType.LCURLY ) ) {
				retVal = new VoidExpressionNode( getContext() );
			} else {
				// set start and end line for error
				setStartLine();
				setEndLine();
				// Using with scope to get the correct context
				throwExceptionWithScope( "expected expression", "", "" );
			}
		}

		if( token.is( Scanner.TokenType.LCURLY ) ) {
			return parseInlineTreeExpression( retVal );
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
		ParsingContext ctx =
			new URIParsingContext( rootExpression.context().source(), rootExpression.context().startLine(),
				getContext().endLine(), rootExpression.context().startColumn(), getContext().endColumn(), codeLine() );
		eat( Scanner.TokenType.RCURLY, "expected }" );

		return new InlineTreeExpressionNode(
			ctx,
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

	private enum ExtendedIdentifierState {
		CAN_READ_ID, CANNOT_READ_ID, STOP
	}

	private String parseExtendedIdentifier( String errorMessage,
		Scanner.TokenType... extensions )
		throws IOException, ParserException {
		List< String > importTargetComponents = new ArrayList<>();
		ExtendedIdentifierState state = ExtendedIdentifierState.CAN_READ_ID;
		while( state != ExtendedIdentifierState.STOP ) {
			if( state == ExtendedIdentifierState.CAN_READ_ID && token.isIdentifier() ) {
				importTargetComponents.add( token.content() );
				nextToken();
				state = ExtendedIdentifierState.CANNOT_READ_ID;
			} else if( Arrays.stream( extensions ).anyMatch( extension -> token.is( extension ) ) ) {
				importTargetComponents.add( token.content() );
				nextToken();
				state = ExtendedIdentifierState.CAN_READ_ID;
			} else {
				state = ExtendedIdentifierState.STOP;
			}
		}
		String id = importTargetComponents.stream().collect( Collectors.joining() );
		if( id.isEmpty() ) {
			setEndLine(); // set end line for error
			throwExceptionWithScope( errorMessage, null, "import" );
		}
		return id;
	}

	private void parseImport() throws IOException, ParserException {
		if( token.is( Scanner.TokenType.FROM ) ) {
			ParsingContext context = getContext();
			boolean isNamespaceImport = false;
			nextToken();
			List< String > importTargets = new ArrayList<>();
			boolean importTargetIDStarted = false;
			List< Pair< String, String > > pathNodes = null;
			boolean keepRun = true;
			do {
				if( token.is( Scanner.TokenType.IMPORT ) ) {
					keepRun = false;
					nextToken();
				} else if( token.is( Scanner.TokenType.DOT ) ) {
					if( !importTargetIDStarted ) {
						importTargets.add( token.content() );
					}
					nextToken();
				} else {
					setEndLine(); // set end line for error
					importTargets.add(
						parseExtendedIdentifier( "expected identifier for importing target after from",
							Scanner.TokenType.MINUS, Scanner.TokenType.AT ) );
					importTargetIDStarted = true;
					// nextToken();
				}
			} while( keepRun );

			if( token.is( Scanner.TokenType.ASTERISK ) ) {
				isNamespaceImport = true;
				nextToken();
			} else {
				setEndLine(); // set end line for eventual error
				assertIdentifier( "expected Identifier or * after import" );
				pathNodes = new ArrayList<>();
				keepRun = false;
				do {
					String targetName = token.content();
					String localName = targetName;
					nextToken();
					if( token.is( Scanner.TokenType.AS ) ) {
						nextToken();
						setEndLine(); // set end line for eventual error
						assertIdentifier( "expected Identifier after as" );
						localName = token.content();
						nextToken();
					}

					pathNodes.add( new Pair< String, String >( targetName, localName ) );
					if( token.is( Scanner.TokenType.COMMA ) ) {
						keepRun = true;
						nextToken();
					} else {
						keepRun = false;
					}
				} while( keepRun );
			}
			setEndLine(); // set end line for eventual error
			ImportStatement stmt = null;
			if( isNamespaceImport ) {
				stmt = new ImportStatement( context, Collections.unmodifiableList( importTargets ) );
			} else {
				stmt = new ImportStatement( context, Collections.unmodifiableList( importTargets ),
					Collections.unmodifiableList( pathNodes ) );
			}
			programBuilder.addChild( stmt );
			return;
		}
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
