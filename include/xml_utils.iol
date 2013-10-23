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
		.includeAttributes?:bool // Default: true
	}
}

type ValueToXmlRequest: void {
  .root: any { ? }  			
  .rootNodeName: string
}

interface XmlUtilsInterface{
	RequestResponse:
		xmlToValue( XMLToValueRequest )(undefined) throws IOException(IOExceptionType),
		transform( XMLTransformationRequest )(string) throws TransformerException(JavaExceptionType),
		/**!
		*  It transforms the value contained within the root node into an xml string. Root values of field .root will be discarded
		*/
		valueToXml( ValueToXmlRequest )(string)
}

outputPort XmlUtils {
	Interfaces: XmlUtilsInterface
}

embedded {
Java:
	"joliex.util.XmlUtils" in XmlUtils
}
