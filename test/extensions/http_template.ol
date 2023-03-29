/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2022 by Balint Maschio <bmaschio77@gmail.com            *
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


from .private.http_template_interface import HttpTemplateInterface
from .private.http_template_server import HttpTemplateServer
from ..test-unit import TestUnitInterface
from string_utils import StringUtils
from console import Console


service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

    outputPort TestHttpTemplateB{
        Location : "socket://localhost:9099"
    }

    outputPort TestHttpTemplate {
        interfaces: HttpTemplateInterface
        protocol: "http"{
           osc.getOrder.template="/api/orders/{id}"
           osc.getOrder.method="GET"
           osc.getOrder.outHeaders.("Authorization")= "token"
           osc.getOrders.template="/api/orders"
           osc.getOrders.method="GET"
           osc.getOrders.outHeaders.("Authorization")= "token"
           osc.addOrder.template="/api/orders"
           osc.addOrder.method="POST"
           osc.addOrder.outHeaders.("Authorization")= "token"
           osc.addOrder.statusCodes.IOException = 500
        }
        Location : "socket://localhost:9099"
    }

    embed StringUtils as stringUtils
    embed Console as console
    embed HttpTemplateServer in TestHttpTemplateB


	main {
		test()() {
			/*
			* Write the code of your test here (replace nullProcess),
			* and replace the first line of the copyright header with your data.
			*
			* The test is supposed to throw a TestFailed fault in case of a failure.
			* You should add a description that reports what the failure was about,
			* for example:
			*
			* throw( TestFailed, "string concatenation does not match correct result" )
			*/
			addOrder@TestHttpTemplate({token="sometoken"
                                       ammount = 10.0})()
            addOrder@TestHttpTemplate({token="sometoken"
                                       ammount = 11.0})()
            addOrder@TestHttpTemplate({token="sometoken"
                                       ammount = 21.0})()
            getOrders@TestHttpTemplate({token="sometoken"})(resultGetOrders)
            if (#resultGetOrders.orders!=3){
                throw( TestFailed, "wrong number of results in getOrders" )
            }
            request.token = "sometoken"
            request.id = resultGetOrders.orders[2].id
            getOrder@TestHttpTemplate(request)(resultGetOrder)

            if(resultGetOrders.orders[2].id != resultGetOrder.id){
                throw( TestFailed, "wrong id" )
            }

		}
	}
}