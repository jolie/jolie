/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

package jolie.xml;

import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import jolie.lang.Constants;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utilities for interactions and transformations with XML.
 * @author Fabrizio Montesi
 */
public class XmlUtils
{
	private static final String JOLIE_TYPE_ATTRIBUTE = "_jolie_type";

	/**
	 * Transforms a jolie.Value object to an XML Document instance preserving types.
	 * @see Document
	 * @param value the source Value
	 * @param rootNodeName the name to give to the root node of the document
	 * @param document the XML document receiving the transformation
	 * @author Claudio Guidi 7/1/2011
	 */
	public static void valueToStorageDocument( Value value, String rootNodeName, Document document )
	{
		Element root = document.createElement( rootNodeName );
		document.appendChild( root );
		_valueToStorageDocument( value, root, document );
	}

	/**
	 * Transforms a jolie.Value object to an XML Document instance.
	 * @see Document
	 * @param value the source Value
	 * @param rootNodeName the name to give to the root node of the document
	 * @param document the XML document receiving the transformation
	 */
	public static void valueToDocument( Value value, String rootNodeName, Document document )
	{
		Element root = document.createElement( rootNodeName );
		document.appendChild( root );
		_valueToDocument( value, root, document );
	}

	/**
	 * Transforms a jolie.Value object to an XML Document instance.
	 * @see Document
	 * @param value the source Value
	 * @param element the root element where to start
	 * @param document the XML document receiving the transformation
	 */
	public static void valueToDocument( Value value, Element element, Document document ) {
		_valueToDocument( value, element, document );
	}

	/**
	 * Transforms a jolie.Value object to an XML Document instance following a given XML Type Definition.
	 * @see Document
	 * @param value the source Value
	 * @param rootNodeName the name to give to the root node of the document.
	 * @param document the XML document receiving the transformation.
	 * @param type the XML type definition to follow in writing the XML document.
	 */
	public static void valueToDocument( Value value, String rootNodeName, Document document, XSType type )
	{
		Element root = document.createElement( rootNodeName );
		document.appendChild( root );
		_valueToDocument( value, root, document, type );
	}


	private static void _valueToDocument( Value value, Element element, Document doc, XSModelGroup modelGroup )
	{
		String name;
		XSModelGroup.Compositor compositor = modelGroup.getCompositor();
		if ( compositor.equals( XSModelGroup.SEQUENCE ) ) {
			XSParticle[] children = modelGroup.getChildren();
			XSTerm currTerm;
			XSElementDecl currElementDecl;
			Value v;
			ValueVector vec;
			for( int i = 0; i < children.length; i++ ) {
				currTerm = children[i].getTerm();
				if ( currTerm.isElementDecl() ) {
					currElementDecl = currTerm.asElementDecl();
					name = currElementDecl.getName();
					Element childElement = null;
					if ( (vec=value.children().get( name )) != null ) {
						int k = 0;
						while(
							vec.isEmpty() == false &&
							(children[i].getMaxOccurs() == XSParticle.UNBOUNDED ||
								children[i].getMaxOccurs() > k)
						) {
							childElement = doc.createElement( name );
							element.appendChild( childElement );
							v = vec.remove( 0 );
							_valueToDocument( v, childElement, doc, currElementDecl.getType() );
							k++;
						}
					} else if ( children[i].getMinOccurs() > 0 ) {
						// TODO throw some error here
					}
				} else if ( currTerm.isModelGroupDecl() ) {
					_valueToDocument( value, element, doc, currTerm.asModelGroupDecl().getModelGroup() );
				} else if ( currTerm.isModelGroup() ) {
					_valueToDocument( value, element, doc, currTerm.asModelGroup() );
				}
			}
		} else if ( compositor.equals( XSModelGroup.CHOICE ) ) {
			XSParticle[] children = modelGroup.getChildren();
			XSTerm currTerm;
			XSElementDecl currElementDecl;
			Value v;
			ValueVector vec;
			boolean found = false;
			for( int i = 0; i < children.length && !found; i++ ) {
				currTerm = children[i].getTerm();
				if ( currTerm.isElementDecl() ) {
					currElementDecl = currTerm.asElementDecl();
					name = currElementDecl.getName();
					Element childElement = null;
					if ( (vec=value.children().get( name )) != null ) {
						childElement = doc.createElement( name );
						element.appendChild( childElement );
						found = true;
						v = vec.remove( 0 );
						_valueToDocument( v, childElement, doc, currElementDecl.getType() );
					} else if ( children[i].getMinOccurs() > 0 ) {
						// TODO throw some error here
					}
				}
			}
		}
	}

	public static void documentToValue( Document document, Value value )
	{
		documentToValue( document, value, true );
	}
	
	private static void _valueToDocument( Value value, Element element, Document doc, XSType type )
	{
		if ( type.isSimpleType() ) {
			element.appendChild( doc.createTextNode( value.strValue() ) );
		} else if ( type.isComplexType() ) {
			String name;
			Value currValue;
			XSComplexType complexType = type.asComplexType();

			// Iterate over attributes
			Collection< ? extends XSAttributeUse > attributeUses = complexType.getAttributeUses();
			for( XSAttributeUse attrUse : attributeUses ) {
				name = attrUse.getDecl().getName();
				if ( (currValue=getAttributeOrNull( value, name )) != null ) {
					element.setAttribute( name, currValue.strValue() );
				}
			}

			XSContentType contentType = complexType.getContentType();
			XSParticle particle = contentType.asParticle();
			if ( contentType.asSimpleType() != null ) {
				element.appendChild( doc.createTextNode( value.strValue() ) );
			} else if ( particle != null ) {
				XSTerm term = particle.getTerm();
				XSModelGroupDecl modelGroupDecl;
				XSModelGroup modelGroup = null;
				if ( (modelGroupDecl=term.asModelGroupDecl()) != null ) {
					modelGroup = modelGroupDecl.getModelGroup();
				} else if ( term.isModelGroup() ) {
					modelGroup = term.asModelGroup();
				}
				if ( modelGroup != null ) {
					_valueToDocument( value, element, doc, modelGroup );
				}
			}
		}
	}

	private static void _valueToDocument(
			Value value,
			Element element,
			Document doc
	) {
		element.appendChild( doc.createTextNode( value.strValue() ) );
		Map< String, ValueVector > attrs = getAttributesOrNull( value );
		if ( attrs != null ) {
			for( Entry< String, ValueVector > attrEntry : attrs.entrySet() ) {
				element.setAttribute(
					attrEntry.getKey(),
					attrEntry.getValue().first().strValue()
				);
			}
		}

		Element currentElement;
		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( !entry.getKey().startsWith( "@" ) ) {
				for( Value val : entry.getValue() ) {
					currentElement = doc.createElement( entry.getKey() );
					element.appendChild( currentElement );
					_valueToDocument( val, currentElement, doc );
				}
			}
		}
	}

	/*
	 * author Claudio Guidi
	 * 7/1/2011
	 */
	private static void _valueToStorageDocument(
		Value value,
		Element element,
		Document doc
	) {
		// Supports only string, int, double and bool
		if ( value.isString() ) {
			element.appendChild( doc.createTextNode( value.strValue() ) );
			element.setAttribute( JOLIE_TYPE_ATTRIBUTE, "string" );
		} else if ( value.isInt() ) {
			element.appendChild( doc.createTextNode( Integer.toString( value.intValue() ) ) );
			element.setAttribute( JOLIE_TYPE_ATTRIBUTE, "int" );
		} else if ( value.isDouble() ) {
			element.appendChild( doc.createTextNode( Double.toString( value.doubleValue() ) ) );
			element.setAttribute( JOLIE_TYPE_ATTRIBUTE, "double" );
		} else if ( value.isLong() ) {
			element.appendChild( doc.createTextNode( Long.toString( value.longValue() ) ) );
			element.setAttribute( JOLIE_TYPE_ATTRIBUTE, "long" );
		} else if ( value.isBool() ) {
			element.appendChild( doc.createTextNode( Boolean.toString( value.boolValue() ) ) );
			element.setAttribute( JOLIE_TYPE_ATTRIBUTE, "bool" );
		} else {
			element.setAttribute( JOLIE_TYPE_ATTRIBUTE, "void" );
		}

		// adding other attributes
		Map<String, ValueVector> attrs = getAttributesOrNull( value );
		if ( attrs != null ) {
			for( Entry<String, ValueVector> attrEntry : attrs.entrySet() ) {
				element.setAttribute(
					attrEntry.getKey(),
					attrEntry.getValue().first().strValue() );
			}
		}

		// adding subelements
		Element currentElement;
		for( Entry<String, ValueVector> entry : value.children().entrySet() ) {
			if ( !entry.getKey().startsWith( "@" ) ) {
				for( Value val : entry.getValue() ) {
					currentElement = doc.createElement( entry.getKey() );
					element.appendChild( currentElement );
					_valueToStorageDocument( val, currentElement, doc );
				}
			}
		}
	}

	public static Map< String, ValueVector > getAttributesOrNull( Value value )
	{
		Map< String, ValueVector > ret = null;
		ValueVector vec = value.children().get( Constants.Predefined.ATTRIBUTES.token().content() );
		if ( vec != null && vec.size() > 0 ) {
			ret = vec.first().children();
		}

		if ( ret == null ) {
			ret = new HashMap< String, ValueVector >();
		}

		return ret;
	}

	/**
	 * Transforms an XML Document to a Value representation
	 * @see Document
	 * @param document the source XML document
	 * @param value the Value receiving the JOLIE representation of document
	 */
	public static void documentToValue( Document document, Value value, boolean includeAttributes )
	{ 
		if ( includeAttributes ) {
			setAttributes( value, document.getDocumentElement() );
			elementsToSubValues(
				value,
				document.getDocumentElement().getChildNodes(),
				true
			);
		} else {
			elementsToSubValues(
				value,
				document.getDocumentElement().getChildNodes(),
				false
			);
		}
	}
        

	/*
	 * author: Claudio Guidi
	 * 7/1/2011
	 */
	public static void storageDocumentToValue( Document document, Value value )
	{
		String type = setAttributesForStoring( value, document.getDocumentElement() );
		elementsToSubValuesForStoring(
			value,
			document.getDocumentElement().getChildNodes(),
			type
		);
	}
	
	private static Value getAttribute( Value value, String attrName )
	{
		return value.getFirstChild( Constants.Predefined.ATTRIBUTES.token().content() )
					.getFirstChild( attrName );
	}

	private static Value getAttributeOrNull( Value value, String attributeName )
	{
		Value ret = null;
		Map< String, ValueVector > attrs = getAttributesOrNull( value );
		if ( attrs != null ) {
			ValueVector vec = attrs.get( attributeName );
			if ( vec != null && vec.size() > 0 ) {
				ret = vec.first();
			}
		}

		return ret;
	}
	
	private static void setAttributes( Value value, Node node )
	{
		NamedNodeMap map = node.getAttributes();
		if ( map != null ) {
			Node attr;
			for( int i = 0; i < map.getLength(); i++ ) {
				attr = map.item( i );
				getAttribute( value, ( attr.getLocalName() == null ) ? attr.getNodeName() : attr.getLocalName() ).setValue( attr.getNodeValue() );
			}
		}
	}

	/*
	 * author Claudio Guidi
	 * 7/1/2011
	 * @return the type of the JOLIE_TYPE
	 */
	private static String setAttributesForStoring( Value value, Node node )
	{
		NamedNodeMap map = node.getAttributes();
		String type = "string";
		if ( map != null ) {
			Node attr;
			for( int i = 0; i < map.getLength(); i++ ) {
				attr = map.item( i );
				if ( !attr.getNodeName().equals( JOLIE_TYPE_ATTRIBUTE ) ) {  // do not consider attribute type
					getAttribute( value, (attr.getLocalName() == null) ? attr.getNodeName() : attr.getLocalName() ).setValue( attr.getNodeValue() );
				} else {
					type = attr.getNodeValue();
				}
			}
		}
		return type;
	}

	/*
	 * author Claudio Guidi
	 * 7/1/2011
	 */
	private static void elementsToSubValuesForStoring( Value value, NodeList list, String type )
	{
		Node node;
		Value childValue;
		StringBuilder builder = new StringBuilder();
		for( int i = 0; i < list.getLength(); i++ ) {
			node = list.item( i );
			switch( node.getNodeType() ) {
				case Node.ATTRIBUTE_NODE:
					if ( !node.getNodeName().equals( JOLIE_TYPE_ATTRIBUTE ) ) {
						getAttribute( value, node.getNodeName() ).setValue( node.getNodeValue() );
					}
					break;
				case Node.ELEMENT_NODE:
					childValue = value.getNewChild( (node.getLocalName() == null) ? node.getNodeName() : node.getLocalName() );
					String subElType = setAttributesForStoring( childValue, node );
					elementsToSubValuesForStoring( childValue, node.getChildNodes(), subElType );
					break;
				case Node.CDATA_SECTION_NODE:
				case Node.TEXT_NODE:
					builder.append( node.getNodeValue() );
					break;
			}
		}

		if ( type.equals( "string" ) ) {
			value.setValue( builder.toString() );
		} else if ( type.equals( "int" ) ) {
			value.setValue( new Integer( builder.toString() ) );
		} else if ( type.equals( "long" ) ) {
			value.setValue( new Long( builder.toString() ) );
		} else if ( type.equals( "double" ) ) {
			value.setValue( new Double( builder.toString() ) );
		} else if ( type.equals( "bool" ) ) {
			value.setValue( new Boolean( builder.toString() ) );
		}
	}

	private static void elementsToSubValues( Value value, NodeList list, boolean includeAttributes )
	{
		Node node;
		Value childValue;
		StringBuilder builder = new StringBuilder();
		for( int i = 0; i < list.getLength(); i++ ) {
			node = list.item( i );
			switch( node.getNodeType() ) {
			case Node.ATTRIBUTE_NODE:
				if ( includeAttributes ) {
					getAttribute( value, node.getNodeName() ).setValue( node.getNodeValue() );
				}
				break;
			case Node.ELEMENT_NODE:
				childValue = value.getNewChild( ( node.getLocalName() == null ) ? node.getNodeName() : node.getLocalName() );
				if ( includeAttributes ){
					setAttributes( childValue, node );
				}
				elementsToSubValues( childValue, node.getChildNodes(), includeAttributes );
				break;
			case Node.CDATA_SECTION_NODE:
			case Node.TEXT_NODE:
				builder.append( node.getNodeValue() );
				break;
			}
		}
		if ( builder.length() > 0 ) {
			value.setValue( builder.toString() );
		}
	}
}
