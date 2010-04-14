/***************************************************************************
 *   Copyright (C) 2010 by Mirco Gamberini                                 *
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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
import com.sun.xml.xsom.XSSchemaSet;
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
import jolie.lang.parse.ParsingContext;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.util.Range;
import jolie.xml.xsd.XsdToJolieConverter;
import jolie.xml.xsd.XsdUtils;

/**
 * @author Mirco Gamberini
 * @author Fabrizio Montesi
 */
public class XsdToJolieConverterImpl implements XsdToJolieConverter
{
	private final Logger logger;
	private final List< TypeDefinition > jolieTypes = new ArrayList< TypeDefinition >();
	private final static ParsingContext parsingContext = new ParsingContext();
	private final boolean strict;
	private final XSSchemaSet schemaSet;
	private final Map< String, TypeDefinition > complexTypes = new HashMap< String, TypeDefinition >();

	private static final String WARNING_1 = "Element type does not exist.";
	private static final String WARNING_2 = "The referred type is not defined or is not supported by JOLIE.";
	private static final String ERROR_SIMPLE_TYPE = "\nERROR: ConversionException\n";
	private static final String WARNING_SIMPLE_TYPE = "The following simple type can not be converted to a suitable JOLIE type: ";
	private static final String WARNING_CONVERT_STRING = "Simple type converted to \"string\" for compatibility reasons.";
	private static final String ERROR_CHOICE = "Element \"choice\" is unsupported by JOLIE. Consider using \"all\" instead.";
	private static final String WARNING_SEQUENCE = "Element \"sequence\" is unsupported by JOLIE.";
	private static final String WARNING_DEFAULT_ATTRIBUTE = "Attribute \"default\" is unsupported by JOLIE.";
	private static final String WARNING_FIXED_ATTRIBUTE = "Attribute \"fixed\" is unsupported by JOLIE.";

	/*
	 * Constructor.
	 * @param schemaSet the schema set to convert.
	 * @param strict {@code true} if encountering elements unsupported by JOLIE should raise an exception, {@code false} if they should just raise a warning message.
	 */
	public XsdToJolieConverterImpl( XSSchemaSet schemaSet, boolean strict, Logger logger )
	{
		this.strict = strict;
		this.schemaSet = schemaSet;
		this.logger = logger;
	}

	public List< TypeDefinition > convert()
		throws ConversionException
	{
		// Load complex types
		Iterator complexIter = schemaSet.iterateComplexTypes();
		while( complexIter.hasNext() ) {
			XSComplexType complexType = (XSComplexType) complexIter.next();
			if ( complexType.getName().equals( "anyType" ) == false ) {
				TypeDefinition jolieComplexType = loadComplexType( complexType );
				complexTypes.put( jolieComplexType.id(), jolieComplexType );
				jolieTypes.add( jolieComplexType );
			}
		}

		// Load element types
		Iterator elementDeclIter = schemaSet.iterateElementDecls();
		while( elementDeclIter.hasNext() ) {
			XSElementDecl element = (XSElementDecl) elementDeclIter.next();
			XSType type = element.getType();

			checkDefaultAndFixed( element );

			if ( type == null ) {
				continue;
			}

			if ( type.getName() != null && type.getName().equals( "anyType" ) ) {
				continue;
			}

			// The element refers to a previously defined complex type
			if ( type.getName() != null && complexTypes.get( type.getName() ) != null && complexTypes.containsKey( element.getName() ) == false ) {
				TypeDefinition jolieType = new TypeDefinitionLink( parsingContext, element.getName(), Constants.RANGE_ONE_TO_ONE, complexTypes.get( type.getName() ) );
				jolieTypes.add( jolieType );
				continue;
			}

			if ( type.isSimpleType() ) { // Element is a simple type
				checkForNativeType( type, WARNING_1 );
			} else if ( type.isComplexType() ) { // Element is a complex type
				XSComplexType complexType = type.asComplexType();
				XSParticle particle;
				XSContentType contentType;
				contentType = complexType.getContentType();
				
				// The complex type does not contain sub elements
				if ( (particle = contentType.asParticle()) == null ) {
					jolieTypes.add( createAnyOrUndefined( element.getName(), complexType ) );
					continue;
				}
				
				if ( contentType.asSimpleType() != null ) { // Unsupported by JOLIE
					checkStrictModeForSimpleType( contentType );
				} else if ( (particle = contentType.asParticle()) != null ) {
					XSTerm term = particle.getTerm();
					XSModelGroupDecl modelGroupDecl = null;
					XSModelGroup modelGroup = null;
					modelGroup = getModelGroup( modelGroupDecl, term );
					if ( modelGroup != null ) {
						XSModelGroup.Compositor compositor = modelGroup.getCompositor();
						// We handle "all" and "sequence", but not "choice"
						if ( compositor.equals( XSModelGroup.ALL ) || compositor.equals( XSModelGroup.SEQUENCE ) ) {
							if ( compositor.equals( XSModelGroup.SEQUENCE ) ) {
								log( Level.WARNING, WARNING_SEQUENCE );
							}

							// Create the new complex type
							TypeInlineDefinition jolieComplexType = createComplexType( complexType, element.getName(), particle );
							navigateSubTypes( modelGroup.getChildren(), jolieComplexType );
							jolieTypes.add( jolieComplexType );
						} else if ( compositor.equals( XSModelGroup.CHOICE ) ) {
							throw new ConversionException( ERROR_CHOICE );
						}
					}
				}
			} else {
				log( Level.WARNING, "Found a type that is not simple nor complex." );
			}
		}
		return jolieTypes;
	}

	private void log( Level level, String message )
	{
		if ( logger != null ) {
			logger.log( level, message );
		}
	}

	/**
	 * Recursive navigation of a complex type sub elements
	 */
	private void navigateSubTypes( XSParticle[] children, TypeInlineDefinition jolieType )
		throws ConversionException
	{
		XSTerm currTerm;
		XSElementDecl currElementDecl;
		for( int i = 0; i < children.length; i++ ) {
			currTerm = children[i].getTerm();
			if ( currTerm.isElementDecl() ) {
				currElementDecl = currTerm.asElementDecl();
				XSType type = currElementDecl.getType();
				/*if ( type != null && type.getName() != null && type.getName().contentEquals( "anyType" ) ) {
					TypeInlineDefinition jolieSimpleType = new TypeInlineDefinition( parsingContext, currElementDecl.getName(), XsdUtils.xsdToNativeType( type.getName() ), getRange( children[i] ) );
					jolieType.putSubType( jolieSimpleType );
					continue;
				}*/
				if ( type != null && type.getName() != null && complexTypes.get( type.getName() ) != null ) {
					TypeDefinition jolieSimpleType = new TypeDefinitionLink( parsingContext, currElementDecl.getName(), getRange( children[i] ), complexTypes.get( type.getName() ) );
					jolieType.putSubType( jolieSimpleType );
					continue;
				}

				checkDefaultAndFixed( currElementDecl );
				if ( type.isSimpleType() ) {
					checkForNativeType( type, WARNING_2 );
					if ( type.getName() != null && XsdUtils.xsdToNativeType( type.getName() ) != null ) {
						jolieType.putSubType( createSimpleType( type, currElementDecl, getRange( children[i] ) ) );
					}
				} else if ( type.isComplexType() ) {
					XSComplexType complexType = type.asComplexType();
					XSParticle particle;
					XSContentType contentType;
					contentType = complexType.getContentType();
					if ( (particle = contentType.asParticle()) == null ) {
						jolieType.putSubType( createAnyOrUndefined( currElementDecl.getName(), complexType ) );
						continue;
					}
					if ( contentType.asSimpleType() != null ) {
						checkStrictModeForSimpleType( contentType );
					} else if ( (particle = contentType.asParticle()) != null ) {
						
						XSTerm term = particle.getTerm();
						XSModelGroupDecl modelGroupDecl = null;
						XSModelGroup modelGroup = null;
						modelGroup = getModelGroup( modelGroupDecl, term );
						if ( modelGroup != null ) {
							XSModelGroup.Compositor compositor = modelGroup.getCompositor();
							if ( compositor.equals( XSModelGroup.ALL ) || compositor.equals( XSModelGroup.SEQUENCE ) ) {
								if ( compositor.equals( XSModelGroup.SEQUENCE ) ) {
									log( Level.WARNING, WARNING_SEQUENCE );
								}

								// Create a new complex type
								TypeInlineDefinition jolieComplexType = createComplexType( complexType, currElementDecl.getName(), children[i] );
								if ( type.getBaseType().getName().equals( "anyType" ) ) {
									jolieComplexType.setUntypedSubTypes( true );
								} else {
									navigateSubTypes( modelGroup.getChildren(), jolieComplexType );
								}
								jolieType.putSubType( jolieComplexType );
							} else if ( compositor.equals( XSModelGroup.CHOICE ) ) {
								throw new ConversionException( ERROR_CHOICE );
							}
						}
					}
				} else {
					log( Level.WARNING, "Rilevato tipo diverso da simple o complex" );
				}
			}
		}
	}

	private TypeDefinition loadComplexType( XSComplexType complexType )
		throws ConversionException
	{
		XSParticle particle;
		XSContentType contentType;
		contentType = complexType.getContentType();
		if ( (particle = contentType.asParticle()) == null ) {
			return createAnyOrUndefined( complexType.getName(), complexType );
		}
		TypeInlineDefinition jolieType = createAnyOrUndefined( complexType.getName(), complexType );
		if ( contentType.asSimpleType() != null ) {
			checkStrictModeForSimpleType( contentType );
		} else if ( (particle = contentType.asParticle()) != null ) {
			XSTerm term = particle.getTerm();
			XSModelGroupDecl modelGroupDecl = null;
			XSModelGroup modelGroup = null;
			modelGroup = getModelGroup( modelGroupDecl, term );
			if ( modelGroup != null ) {
				XSModelGroup.Compositor compositor = modelGroup.getCompositor();
				if ( compositor.equals( XSModelGroup.ALL ) || compositor.equals( XSModelGroup.SEQUENCE ) ) {
					if ( compositor.equals( XSModelGroup.SEQUENCE ) ) {
						log( Level.WARNING, WARNING_SEQUENCE );
					}
					XSTerm currTerm;
					XSElementDecl currElementDecl;
					for( int i = 0; i < modelGroup.getChildren().length; i++ ) {
						currTerm = modelGroup.getChildren()[i].getTerm();
						if ( currTerm.isElementDecl() ) {
							currElementDecl = currTerm.asElementDecl();
							XSType type = currElementDecl.getType();
							if ( type != null && type.getName() != null && type.getName().contentEquals( "anyType" ) ) {
								TypeInlineDefinition simpleType = new TypeInlineDefinition( parsingContext, currElementDecl.getName(), XsdUtils.xsdToNativeType( type.getName() ), getRange( modelGroup.getChildren()[i] ) );
								jolieType.putSubType( simpleType );
								continue;
							}

							checkDefaultAndFixed( currElementDecl );
							if ( type.isSimpleType() ) {
								checkForNativeType( type, WARNING_2 );
								if ( type.getName() != null ) {
									if ( XsdUtils.xsdToNativeType( type.getName() ) != null ) {
										jolieType.putSubType( createSimpleType( type, currElementDecl, getRange( modelGroup.getChildren()[i] ) ) );
									}
								}
							} else if ( type.isComplexType() ) {
								TypeDefinition jolieComplexType = null;
								if ( type.isGlobal() ) {
									jolieComplexType = new TypeDefinitionLink( parsingContext, currElementDecl.getName(), getRange( modelGroup.getChildren()[i] ), new TypeInlineDefinition( parsingContext, type.getName(), NativeType.VOID, Constants.RANGE_ONE_TO_ONE ) );
								} else {
									jolieComplexType = createComplexType( complexType, currElementDecl.getName(), modelGroup.getChildren()[i] );
									loadComplexType( type.asComplexType() );
								}
								jolieType.putSubType( jolieComplexType );
							}
						}
					}
				} else if ( compositor.equals( XSModelGroup.CHOICE ) ) {
					throw new ConversionException( ERROR_CHOICE );
				}
			}
		}
		return jolieType;
	}

	private TypeInlineDefinition createAnyOrUndefined( String typeName, XSComplexType complexType )
	{
		TypeInlineDefinition jolieType = new TypeInlineDefinition( parsingContext, typeName, NativeType.ANY, Constants.RANGE_ONE_TO_ONE );
		if ( !complexType.isMixed() ) {
			jolieType.setUntypedSubTypes( true );
		}
		return jolieType;
	}

	private void checkType( XSType type )
	{
		if ( type.getName() != null && (type.getName().contains( "date" ) || type.getName().contains( "time" ) || type.getName().contains( "boolean" )) ) {
			log( Level.WARNING, WARNING_CONVERT_STRING + " Type: " + type.getName() );
		}
	}

	/**
	 * Emit an alert in case we find a "default" or "fixed" attribute
	 */
	private void checkDefaultAndFixed( XSElementDecl element )
	{
		if ( element.getDefaultValue() != null ) {
			log( Level.WARNING, WARNING_DEFAULT_ATTRIBUTE + " Element: " + element.getName() );
		}

		if ( element.getFixedValue() != null ) {
			log( Level.WARNING, WARNING_FIXED_ATTRIBUTE + " Element: " + element.getName() );
		}

	}

	private TypeInlineDefinition createSimpleType( XSType type, XSElementDecl element, Range range )
	{
		checkType( type );
		return new TypeInlineDefinition( parsingContext, element.getName(), XsdUtils.xsdToNativeType( type.getName() ), range );
	}

	private TypeInlineDefinition createComplexType( XSComplexType complexType, String typeName, XSParticle particle )
	{
		if ( complexType.isMixed() ) {
			return new TypeInlineDefinition( parsingContext, typeName, NativeType.ANY, getRange( particle ) );
		} else {
			return new TypeInlineDefinition( parsingContext, typeName, NativeType.VOID, getRange( particle ) );
		}
	}

	private XSModelGroup getModelGroup( XSModelGroupDecl modelGroupDecl, XSTerm term )
	{
		if ( (modelGroupDecl = term.asModelGroupDecl()) != null ) {
			return modelGroupDecl.getModelGroup();
		} else if ( term.isModelGroup() ) {
			return term.asModelGroup();
		} else {
			return null;
		}
	}

	private boolean strict()
	{
		return strict;
	}

	/**
	 * Checks whether a native type for a given simple type is defined.
	 */
	private void checkForNativeType( XSType type, String msg )
		throws ConversionException
	{
		if ( XsdUtils.xsdToNativeType( type.getName() ) == null ) {
			if ( !strict() ) {
				log( Level.WARNING, msg + " Name: " + type.getName() );
			} else {
				throw new ConversionException( ERROR_SIMPLE_TYPE + msg + " Name: " + type.getName() );
			}
		}
	}

	private void checkStrictModeForSimpleType( XSContentType contentType )
		throws ConversionException
	{
		if ( !strict() ) {
			log( Level.WARNING, WARNING_SIMPLE_TYPE + contentType.asSimpleType().getName() );
		} else {
			throw new ConversionException( ERROR_SIMPLE_TYPE + WARNING_SIMPLE_TYPE + contentType.asSimpleType().getName() );
		}
	}

	private Range getRange( XSParticle part )
	{
		int min = 1;
		int max = Integer.MAX_VALUE;
		if ( part.getMinOccurs() != -1 ) {
			min = part.getMinOccurs();
		}

		if ( part.getMaxOccurs() != -1 ) {
			max = part.getMaxOccurs();
		}

		return new Range( min, max );
	}
}
