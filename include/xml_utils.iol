/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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


include "types/JavaException.iol"
include "types/IOException.iol"

// TODO: fault typing in the Java code

type XMLTransformationRequest:void {
	.source:string
	.xslt:string
}

type XMLToValueRequest:any {
	.options?:void {
		.includeAttributes?:bool // Default: false
		.schemaUrl?:string // Default: none
		.schemaLanguage?:string // Default: "http://www.w3.org/2001/XMLSchema" (see class "SchemaFactory")
		.charset?:string // set the encoding. Default: system (eg. for Unix-like OS UTF-8) or header specification
	}
}

type ValueToXmlRequest: void {
	.root: any { ? }
	.rootNodeName: string
	.plain?:bool // Default: false (= storage XML)
	.omitXmlDeclaration?:bool // Default: false (with XML declaration)
	.indent?:bool // Default: false
}

interface XmlUtilsInterface{
	RequestResponse:
		transform( XMLTransformationRequest )(string) throws TransformerException(JavaExceptionType),
		/**!
		 * Transforms the value contained within the root node into an xml string.
		 *
		 * The base value of ValueToXmlRequest.root will be discarded, the rest gets converted recursively
		 */
		valueToXml( ValueToXmlRequest )(string) throws IOException(IOExceptionType),
		/**!
		 * Transforms the base value in XML format (data types string, raw) into a Jolie value
		 *
		 * The XML root node will be discarded, the rest gets converted recursively
		 */
		xmlToValue( XMLToValueRequest )(undefined) throws IOException(IOExceptionType)
}

outputPort XmlUtils {
	Interfaces: XmlUtilsInterface
}

embedded {
Java:
	"joliex.util.XmlUtils" in XmlUtils
}
