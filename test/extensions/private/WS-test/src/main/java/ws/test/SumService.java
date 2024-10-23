/***************************************************************************
 *   Copyright (C) 2018 by Saverio Giallorenzo                             *
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
 ************************************************************************** */

package ws.test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Endpoint;
import jolie.runtime.JavaService;
import jolie.runtime.embedding.RequestResponse;

@WebService( name = "CalcService", serviceName = "CalcService",
	portName = "CalcServicePort",
	targetNamespace = "http://calc.id" )
@SOAPBinding( style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL )
public class SumService extends JavaService {
	@XmlRootElement( name = "CalculatorWs", namespace = "http://calculator.id" )
	@XmlAccessorType( XmlAccessType.FIELD )
	static public class Calculator {

		static public Integer sum( Integer x, Integer y ) {
			return x + y;
		}

		static public Integer prod( Integer x, Integer y ) {
			return x * y;
		}
	}

	@WebMethod( operationName = "sum", action = "tns:sum" )
	public Integer sum( @WebParam( name = "x" ) Integer x, @WebParam( name = "y" ) Integer y ) {
		return Calculator.sum( x, y );
	}

	@WebMethod( operationName = "prod", action = "tns:prod" )
	public Integer prod( @WebParam( name = "x" ) Integer x, @WebParam( name = "y" ) Integer y ) {
		return Calculator.prod( x, y );
	}

	private static Endpoint e;

	@RequestResponse
	public void start( String url ) {
		url = !url.isEmpty() ? url : "http://localhost:14000/";
		e = Endpoint.publish( url, new SumService() );
	}

	@RequestResponse
	public void close() {
		e.stop();
	}

}
