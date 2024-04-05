/***************************************************************************
 *   Copyright (C) 2010 by Mirco Gamberini                                 *
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2012 by Claudio Guidi <cguidi@italianasoftware.com>     *
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

package jolie.xml.xsd.impl;

import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.BasicTypeDefinition;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;
import jolie.util.Range;
import jolie.xml.xsd.XsdToJolieConverter;
import jolie.xml.xsd.XsdUtils;

/**
 * @author Mirco Gamberini
 * @author Fabrizio Montesi
 */
public class XsdToJolieConverterImpl implements XsdToJolieConverter {
	private final Logger logger;
	private final List< TypeDefinition > jolieTypes = new ArrayList<>();
	private final static ParsingContext PARSING_CONTEXT = URIParsingContext.DEFAULT;
	private final boolean strict;
	private final XSSchemaSet schemaSet;
	private final Map< String, TypeDefinition > complexTypes = new HashMap<>();
	private final Map< String, TypeDefinition > simpleTypes = new HashMap<>();
	private final ArrayList< String > complexTypeNames = new ArrayList<>();
	// private final ArrayList<String> simpleTypeNames = new ArrayList<String>()
	private static final String XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
	private static final String XMLSOAPSCHEMA_URI = "http://schemas.xmlsoap.org/soap/encoding/";
	private static final String WARNING_1 = "Element type does not exist.";
	private static final String WARNING_2 = "The referred type is not defined or is not supported by JOLIE.";
	private static final String ERROR_SIMPLE_TYPE = "\nERROR: ConversionException\n";
	private static final String WARNING_SIMPLE_TYPE =
		"The following simple type can not be converted to a suitable JOLIE type: ";
	private static final String WARNING_CONVERT_STRING =
		"Simple type converted to \"string\" for compatibility reasons.";
	private static final String ERROR_CHOICE =
		"Element \"choice\" is unsupported by JOLIE. Consider using \"all\" instead.";
	private static final String WARNING_SEQUENCE = "Element \"sequence\" is unsupported by JOLIE.";
	private static final String WARNING_DEFAULT_ATTRIBUTE = "Attribute \"default\" is unsupported by JOLIE.";
	private static final String WARNING_FIXED_ATTRIBUTE = "Attribute \"fixed\" is unsupported by JOLIE.";
	public static final String TYPE_SUFFIX = "Type";

	/*
	 * Constructor.
	 * 
	 * @param schemaSet the schema set to convert.
	 * 
	 * @param strict {@code true} if encountering elements unsupported by JOLIE should raise an
	 * exception, {@code false} if they should just raise a warning message.
	 */
	public XsdToJolieConverterImpl( XSSchemaSet schemaSet, boolean strict, Logger logger ) {
		this.strict = strict;
		this.schemaSet = schemaSet;
		this.logger = logger;
	}

	private boolean checkSkippedTypes( String typeName, String targetNameSpace ) {
		boolean flag = false;
		if( targetNameSpace.equals( XMLSCHEMA_URI ) || targetNameSpace.equals( XMLSOAPSCHEMA_URI ) ) {
			if( typeName.equals( "unsignedShort" )
				|| typeName.equals( "long" )
				|| typeName.equals( "date" )
				|| typeName.equals( "float" )
				|| typeName.equals( "short" )
				|| typeName.equals( "nonNegativeInteger" )
				|| typeName.equals( "time" )
				|| typeName.equals( "base64Binary" )
				|| typeName.equals( "gMonthDay" )
				|| typeName.equals( "gYeardDay" )
				|| typeName.equals( "gDay" )
				|| typeName.equals( "gMonth" )
				|| typeName.equals( "gYearMonth" )
				|| typeName.equals( "gYear" )
				|| typeName.equals( "dateTime" )
				|| typeName.equals( "nonPositiveInteger" )
				|| typeName.equals( "anyURI" )
				|| typeName.equals( "byte" )
				|| typeName.equals( "hexBinary" )
				|| typeName.equals( "boolean" )
				|| typeName.equals( "negativeInteger" )
				|| typeName.equals( "unsignedByte" )
				|| typeName.equals( "integer" )
				|| typeName.equals( "int" )
				|| typeName.equals( "unsignedInt" )
				|| typeName.equals( "normalizedString" )
				|| typeName.equals( "double" )
				|| typeName.equals( "decimal" )
				|| typeName.equals( "positiveInteger" )
				|| typeName.equals( "duration" )
				|| typeName.equals( "string" )
				|| typeName.equals( "unsignedLong" )
				|| typeName.equals( "base64" )
				|| typeName.equals( "anyType" )
				|| typeName.equals( "anySimpleType" )
				|| typeName.equals( "ENTITIES" )
				|| typeName.equals( "ENTITY" )
				|| typeName.equals( "ID" )
				|| typeName.equals( "IDREF" )
				|| typeName.equals( "IDREFS" )
				|| typeName.equals( "language" )
				|| typeName.equals( "Name" )
				|| typeName.equals( "NCName" )
				|| typeName.equals( "NMTOKEN" )
				|| typeName.equals( "NMTOKENS" )
				|| typeName.equals( "QName" )
				|| typeName.equals( "token" ) ) {
				flag = true;
			}
		}

		return flag;

	}

	@Override
	public List< TypeDefinition > convert()
		throws ConversionException {

		// creating type name lists
		Iterator< XSComplexType > complexNameIter = schemaSet.iterateComplexTypes();
		while( complexNameIter.hasNext() ) {
			XSComplexType complexType = complexNameIter.next();
			if( complexType.getContentType().asSimpleType() == null
				&& !checkSkippedTypes( complexType.getName(), complexType.getTargetNamespace() ) ) {
				// avoinding simple type insertion
				complexTypeNames.add( complexType.getName().replace( "-", "_" ) + TYPE_SUFFIX );
			}
		}

		// Load simple types
		Iterator< XSSimpleType > simpleIter = schemaSet.iterateSimpleTypes();
		while( simpleIter.hasNext() ) {
			XSSimpleType simpleType = simpleIter.next();
			if( !checkSkippedTypes( simpleType.getName(), simpleType.getTargetNamespace() ) ) {
				TypeDefinition jolieSimpleType;
				jolieSimpleType =
					loadSimpleType( simpleType, false, simpleTypes.get( simpleType.getName() + TYPE_SUFFIX ) );
				simpleTypes.put( jolieSimpleType.name(), jolieSimpleType );
				jolieTypes.add( jolieSimpleType );
			}
		}


		// Load complex types
		Iterator< XSComplexType > complexIter = schemaSet.iterateComplexTypes();
		while( complexIter.hasNext() ) {
			XSComplexType complexType = complexIter.next();
			if( complexType.getContentType().asSimpleType() == null
				&& !checkSkippedTypes( complexType.getName(), complexType.getTargetNamespace() ) ) {
				TypeDefinition jolieComplexType;
				if( complexTypes.containsKey( complexType.getName() + TYPE_SUFFIX ) ) {
					// lazy type
					jolieComplexType =
						loadComplexType( complexType, true, complexTypes.get( complexType.getName() + TYPE_SUFFIX ) );
				} else {
					jolieComplexType =
						loadComplexType( complexType, false, complexTypes.get( complexType.getName() + TYPE_SUFFIX ) );
					if( jolieComplexType != null ) {
						complexTypes.put( jolieComplexType.name(), jolieComplexType );
					}
				}
				if( jolieComplexType != null ) {
					jolieTypes.add( jolieComplexType );
				}
			}
		}


		// Load element types
		Iterator< XSElementDecl > elementDeclIter = schemaSet.iterateElementDecls();
		while( elementDeclIter.hasNext() ) {
			XSElementDecl element = elementDeclIter.next();
			XSType type = element.getType();
			checkDefaultAndFixed( element );


			if( type == null ) {
				continue;
			}

			if( type.getName() != null && type.getName().equals( "anyType" ) ) {
				continue;
			}

			if( type.getName() != null ) {
				String fullName = type.getName() + TYPE_SUFFIX;

				// The element refers to a previously defined complex type
				if( complexTypes.get( fullName ) != null
					&& complexTypes.containsKey( element.getName() ) ) {
					TypeDefinition jolieType = new TypeDefinitionLink( PARSING_CONTEXT, element.getName(),
						Constants.RANGE_ONE_TO_ONE, complexTypes.get( fullName ) );
					jolieTypes.add( jolieType );
					continue;
				}

				// The element refers to a previously defined simple type
				if( simpleTypes.get( fullName ) != null
					&& simpleTypes.containsKey( element.getName() ) == false ) {
					TypeDefinition jolieType = new TypeDefinitionLink( PARSING_CONTEXT, element.getName(),
						Constants.RANGE_ONE_TO_ONE, simpleTypes.get( fullName ) );
					jolieTypes.add( jolieType );
					continue;
				}
			}

			if( type.isSimpleType() ) { // Element is a simple type
				checkForNativeType( type, WARNING_1 );
			} else if( type.isComplexType() ) { // Element is a complex type
				XSComplexType complexType = type.asComplexType();
				XSParticle particle;
				XSContentType contentType;
				contentType = complexType.getContentType();
				// The complex type does not contain sub elements
				if( (particle = contentType.asParticle()) == null
					&& !checkSkippedTypes( element.getName(), element.getTargetNamespace() ) ) {
					jolieTypes.add( createAnyOrUndefined( element.getName(), complexType ) );
					continue;
				}

				if( contentType.asSimpleType() != null ) { // Unsupported by JOLIE
					checkStrictModeForSimpleType( contentType );
				} else if( (particle = contentType.asParticle()) != null ) {
					XSTerm term = particle.getTerm();
					XSModelGroup modelGroup = getModelGroup( term );
					if( modelGroup != null ) {
						TypeInlineDefinition jolieComplexType =
							createComplexType( complexType, element.getName(), particle );
						groupProcessing( modelGroup, particle, jolieComplexType );
						jolieTypes.add( jolieComplexType );
					}
				}
			} else {
				log( Level.WARNING, "Found a type that is not simple nor complex." );
			}
		}
		return jolieTypes;
	}

	private void groupProcessing( XSModelGroup modelGroup, XSParticle particle, TypeInlineDefinition jolieType )
		throws ConversionException {
		XSModelGroup.Compositor compositor = modelGroup.getCompositor();
		// We handle "all" and "sequence", but not "choice"
		if( compositor.equals( XSModelGroup.ALL ) || compositor.equals( XSModelGroup.SEQUENCE ) ) {
			if( compositor.equals( XSModelGroup.SEQUENCE ) ) {
				log( Level.WARNING, WARNING_SEQUENCE );
			}

			for( XSParticle currParticle : modelGroup.getChildren() ) {
				XSTerm currTerm = currParticle.getTerm();
				if( currTerm.isModelGroup() ) {
					groupProcessing( currTerm.asModelGroup(), particle, jolieType );
				} else {
					// Create the new complex type for root types
					navigateSubTypes( currParticle, jolieType );
				}
			}
		} else if( compositor.equals( XSModelGroup.CHOICE ) ) {
			throw new ConversionException( ERROR_CHOICE );
		}

	}

	private void log( Level level, String message ) {
		if( logger != null ) {
			logger.log( level, message );
		}
	}

	private void navigateSubTypes( XSParticle parentParticle, TypeInlineDefinition jolieType )
		throws ConversionException {
		XSTerm currTerm;
		currTerm = parentParticle.getTerm();
		if( currTerm.isElementDecl() ) {
			XSElementDecl currElementDecl;
			currElementDecl = currTerm.asElementDecl();
			XSType type = currElementDecl.getType();

			if( type != null && (type.getName()) != null
				&& complexTypeNames.contains( type.getName() + TYPE_SUFFIX ) ) {
				if( complexTypes.get( type.getName() + TYPE_SUFFIX ) == null ) {
					// create lazy type
					TypeDefinition jolieLazyType = new TypeInlineDefinition( PARSING_CONTEXT,
						type.getName() + TYPE_SUFFIX, BasicTypeDefinition.of( NativeType.ANY ),
						Constants.RANGE_ONE_TO_ONE );

					complexTypes.put( type.getName() + TYPE_SUFFIX, jolieLazyType );
				}
				TypeDefinition jolieSimpleType = new TypeDefinitionLink( PARSING_CONTEXT, currElementDecl.getName(),
					getRange( parentParticle ), complexTypes.get( type.getName() + TYPE_SUFFIX ) );
				jolieType.putSubType( jolieSimpleType );

			} else if( type != null && type.getName() != null
				&& simpleTypes.get( type.getName() + TYPE_SUFFIX ) != null ) {
				/*
				 * if ( simpleTypes.get( type.getName() + TYPE_SUFFIX ) == null ) { // create lazy type
				 * TypeDefinition jolieLazyType = new TypeInlineDefinition( parsingContext, type.getName() +
				 * TYPE_SUFFIX, NativeType.ANY, Constants.RANGE_ONE_TO_ONE ); simpleTypes.put( type.getName() +
				 * TYPE_SUFFIX, jolieLazyType ); }
				 */
				TypeDefinition jolieSimpleType = new TypeDefinitionLink( PARSING_CONTEXT, currElementDecl.getName(),
					getRange( parentParticle ), simpleTypes.get( type.getName() + TYPE_SUFFIX ) );
				jolieType.putSubType( jolieSimpleType );

			} else if( type != null ) {
				checkDefaultAndFixed( currElementDecl );
				if( type.isSimpleType() ) {
					checkForNativeType( type, WARNING_2 );
					if( (type.getName() != null) && XsdUtils.xsdToNativeType( type.getName() ) != null ) {
						jolieType.putSubType( createSimpleType( type, currElementDecl, getRange( parentParticle ) ) );
					}
					if( type.getName() == null && type.asSimpleType().isRestriction() ) {
						XSRestrictionSimpleType restriction = type.asSimpleType().asRestriction();
						checkType( restriction.getBaseType() );
						jolieType.putSubType( createSimpleType( restriction.getBaseType(), currElementDecl,
							Constants.RANGE_ONE_TO_ONE ) );
					}
				} else if( type.isComplexType() ) {
					XSComplexType complexType = type.asComplexType();
					XSParticle particle;
					XSContentType contentType;
					contentType = complexType.getContentType();
					// if( (particle = contentType.asParticle()) == null ) {
					// jolieType.putSubType( createAnyOrUndefined( currElementDecl.getName(), complexType ) );
					// }
					if( contentType.asSimpleType() != null ) {
						checkStrictModeForSimpleType( contentType );
					} else if( (particle = contentType.asParticle()) != null ) {
						XSTerm term = particle.getTerm();
						XSModelGroup modelGroup = getModelGroup( term );
						if( modelGroup != null ) {
							TypeInlineDefinition jolieComplexType =
								createComplexType( complexType, currElementDecl.getName(), particle );
							groupProcessing( modelGroup, particle, jolieComplexType );
							jolieType.putSubType( jolieComplexType );
						}
					}
				}
			}
		}
	}

	private TypeDefinition loadSimpleType( XSSimpleType simpleType, boolean lazy, TypeDefinition lazyType ) {
		// processing restrictions
		TypeInlineDefinition jolietype;

		if( lazy ) {
			jolietype = (TypeInlineDefinition) lazyType;
		} else {
			if( simpleType.isRestriction() ) {
				XSRestrictionSimpleType restriction = simpleType.asRestriction();
				checkType( restriction.getBaseType() );
				jolietype =
					new TypeInlineDefinition( PARSING_CONTEXT, simpleType.getName().replace( "-", "_" ) + TYPE_SUFFIX,
						BasicTypeDefinition.of( XsdUtils.xsdToNativeType( restriction.getBaseType().getName() ) ),
						Constants.RANGE_ONE_TO_ONE );

			} else {
				log( Level.WARNING, "SimpleType not processed:" + simpleType.getName() );
				jolietype = new TypeInlineDefinition( PARSING_CONTEXT, simpleType.getName().replace( "-", "_" ),
					BasicTypeDefinition.of( NativeType.VOID ), Constants.RANGE_ONE_TO_ONE );

			}
		}
		return jolietype;
	}

	private TypeDefinition loadComplexType( XSComplexType complexType, boolean lazy, TypeDefinition lazyType )
		throws ConversionException {
		XSParticle particle;
		XSContentType contentType;
		contentType = complexType.getContentType();

		if( (particle = contentType.asParticle()) == null ) {
			return null;// createAnyOrUndefined( complexType.getName(), complexType );

		}

		TypeInlineDefinition jolieType;

		if( lazy ) {
			jolieType = (TypeInlineDefinition) lazyType;
		} else {
			jolieType =
				createComplexType( complexType, complexType.getName().replace( "-", "_" ) + TYPE_SUFFIX, particle );
		}

		if( contentType.asSimpleType() != null ) {
			checkStrictModeForSimpleType( contentType );

		} else if( (particle = contentType.asParticle()) != null ) {
			XSTerm term = particle.getTerm();
			XSModelGroup modelGroup = getModelGroup( term );
			if( modelGroup != null ) {
				groupProcessing( modelGroup, particle, jolieType );
			}
		}
		return jolieType;


	}

	private TypeInlineDefinition createAnyOrUndefined( String typeName, XSComplexType complexType ) {
		TypeInlineDefinition jolieType =
			new TypeInlineDefinition( PARSING_CONTEXT, typeName, BasicTypeDefinition.of( NativeType.ANY ),
				Constants.RANGE_ONE_TO_ONE );

		if( !complexType.isMixed() ) {
			jolieType.setUntypedSubTypes( true );
		}
		return jolieType;
	}

	private void checkType( XSType type ) {
		if( type.getName() != null && (type.getName().contains( "date" ) || type.getName().contains( "time" )
			|| type.getName().contains( "boolean" )) ) {
			log( Level.WARNING, WARNING_CONVERT_STRING + " Type: " + type.getName() );
		}
	}

	/**
	 * Emit an alert in case we find a "default" or "fixed" attribute
	 */
	private void checkDefaultAndFixed( XSElementDecl element ) {
		if( element.getDefaultValue() != null ) {
			log( Level.WARNING, WARNING_DEFAULT_ATTRIBUTE + " Element: " + element.getName() );
		}

		if( element.getFixedValue() != null ) {
			log( Level.WARNING, WARNING_FIXED_ATTRIBUTE + " Element: " + element.getName() );
		}

	}

	private TypeDefinition createSimpleType( XSType type, XSElementDecl element, Range range ) {
		checkType( type );
		TypeInlineDefinition right = new TypeInlineDefinition( PARSING_CONTEXT, element.getName().replace( "-", "_" ),
			BasicTypeDefinition.of( XsdUtils.xsdToNativeType( type.getName() ) ), range );
		if( element.isNillable() ) {
			TypeInlineDefinition left =
				new TypeInlineDefinition( PARSING_CONTEXT, element.getName().replace( "-", "_" ),
					BasicTypeDefinition.of( NativeType.VOID ), range );
			return new TypeChoiceDefinition( PARSING_CONTEXT, element.getName().replace( "-", "_" ), range, left,
				right );
		} else {
			return right;
		}



	}

	private TypeInlineDefinition createComplexType( XSComplexType complexType, String typeName, XSParticle particle ) {
		if( complexType.isMixed() ) {
			return new TypeInlineDefinition( PARSING_CONTEXT, typeName, BasicTypeDefinition.of( NativeType.ANY ),
				getRange( particle ) );
		} else {
			return new TypeInlineDefinition( PARSING_CONTEXT, typeName, BasicTypeDefinition.of( NativeType.VOID ),
				getRange( particle ) );
		}
	}

	private XSModelGroup getModelGroup( XSTerm term ) {
		XSModelGroupDecl modelGroupDecl;
		if( (modelGroupDecl = term.asModelGroupDecl()) != null ) {
			return modelGroupDecl.getModelGroup();
		} else if( term.isModelGroup() ) {
			return term.asModelGroup();
		} else {
			return null;
		}
	}

	private boolean strict() {
		return strict;
	}

	/**
	 * Checks whether a native type for a given simple type is defined.
	 */
	private void checkForNativeType( XSType type, String msg )
		throws ConversionException {
		if( XsdUtils.xsdToNativeType( type.getName() ) == null ) {
			if( !strict() ) {
				log( Level.WARNING, msg + " Name: " + type.getName() );
			} else {
				throw new ConversionException( ERROR_SIMPLE_TYPE + msg + " Name: " + type.getName() );
			}
		}
	}

	private void checkStrictModeForSimpleType( XSContentType contentType )
		throws ConversionException {
		if( !strict() ) {
			log( Level.WARNING, WARNING_SIMPLE_TYPE + contentType.asSimpleType().getName() );
		} else {
			throw new ConversionException(
				ERROR_SIMPLE_TYPE + WARNING_SIMPLE_TYPE + contentType.asSimpleType().getName() );
		}
	}

	private Range getRange( XSParticle part ) {
		int min = 1;
		int max = Integer.MAX_VALUE;

		if( part.getMinOccurs() != -1 ) {
			min = part.getMinOccurs();
		}

		if( part.getMaxOccurs() != -1 ) {
			max = part.getMaxOccurs();
		}

		return new Range( min, max );
	}
}
