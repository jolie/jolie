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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import jolie.lang.Constants;
import jolie.runtime.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import jolie.runtime.ValueVector;

/**
 * Utilities for interactions and transformations with XML.
 * @author Fabrizio Montesi
 */
public class XmlUtils
{
	/**
	 * Transforms a jolie.Value object to an XML Document instance.
	 * @see Document
	 * @param value the source Value
	 * @param rootNodeName the name to give to the root node of the document
	 * @param document the XML document receiving the transformation
	 */
	static public void valueToDocument( Value value, String rootNodeName, Document document )
	{
		Element root = document.createElement( rootNodeName );
		document.appendChild( root );
		_valueToDocument( value, root, document );
	}

	static private void _valueToDocument(
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
	static public void documentToValue( Document document, Value value )
	{
		setAttributes( value, document.getDocumentElement() );
		elementsToSubValues(
			value,
			document.getDocumentElement().getChildNodes()
		);
	}
	
	private static Value getAttribute( Value value, String attrName )
	{
		return value.getFirstChild( Constants.Predefined.ATTRIBUTES.token().content() )
					.getFirstChild( attrName );
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
	
	private static void elementsToSubValues( Value value, NodeList list )
	{
		Node node;
		Value childValue;
		for( int i = 0; i < list.getLength(); i++ ) {
			node = list.item( i );
			switch( node.getNodeType() ) {
			case Node.ATTRIBUTE_NODE:
				getAttribute( value, node.getNodeName() ).setValue( node.getNodeValue() );
				break;
			case Node.ELEMENT_NODE:
				childValue = value.getNewChild( ( node.getLocalName() == null ) ? node.getNodeName() : node.getLocalName() );
				setAttributes( childValue, node );
				elementsToSubValues( childValue, node.getChildNodes() );
				break;
			case Node.TEXT_NODE:
				value.setValue( node.getNodeValue() );
				break;
			}
		}
	}
}
