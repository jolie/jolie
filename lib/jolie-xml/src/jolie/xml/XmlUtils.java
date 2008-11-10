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

import jolie.Constants;
import jolie.runtime.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utilities for interactions and transformations with XML.
 * @author Fabrizio Montesi
 */
public class XmlUtils
{
	/**
	 * Transforms an XML Document to a Value representation
	 * @see Document
	 * @param document the source XML document
	 * @param value the Value receiving the JOLIE representation of document
	 */
	static public void documentToValue( Document document, Value value )
	{
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
				elementsToSubValues( childValue, node.getChildNodes() ); 
				break;
			case Node.TEXT_NODE:
				value.setValue( node.getNodeValue() );
				break;
			}
		}
	}
}
