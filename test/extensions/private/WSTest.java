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
 ***************************************************************************/

package ws.test;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@WebService( name = "CalcService", serviceName = "CalcService",
	portName = "CalcServicePort",
	targetNamespace = "http://calc.id" )
@SOAPBinding( style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL )
public class WSTest {

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
	
	@WebMethod( operationName = "close", action = "tns:close" )
	public void String () {
		new Thread( new ClosingThread() ).start();
	}
	
	private static Endpoint e;
	
	private class ClosingThread implements Runnable {
		public void run(){
			try {
				Thread.sleep( 250 );
			} catch ( InterruptedException e ){
				e.printStackTrace();
			}
				e.stop();
			}
	}

	public static void main( String[] args ) {
		String url = args.length >0 ? args[0] : "http://localhost:14000/";
		e = Endpoint.publish( url, new WSTest() );
	}

}
