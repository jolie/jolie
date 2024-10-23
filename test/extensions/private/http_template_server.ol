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

from .http_template_interface import HttpTemplateInterface
from string_utils import StringUtils
from console import Console

service HttpTemplateServer{

    inputPort TestHttpTemplate {
        interfaces: HttpTemplateInterface
        protocol: "http"{
           .osc.getOrder.template = "/api/orders/{id}"
           .osc.getOrder.method = "GET"
           .osc.getOrder.inHeaders.Authorization = "token"
           .osc.getOrder.statusCodes.Unauthorized = 403
           .osc.getOrders.template = "/api/orders"
           .osc.getOrders.method = "GET"
           .osc.getOrders.inHeaders.Authorization = "token"
           .osc.getOrders.statusCodes.Unauthorized = 403
           .osc.addOrder.template="/api/orders"
           .osc.addOrder.method="POST"
           .osc.addOrder.inHeaders.Authorization = "token"
           .osc.addOrder.statusCodes = 201
           .osc.addOrder.statusCodes.Unauthorized = 403
           .osc.addOrder.response.headers -> locationHeader
        }
        location : "socket://localhost:9299"
    }
    execution: concurrent

    embed StringUtils as stringUtils
    embed Console as console

    define validateToken {
        if (request.token != "sometoken") {
            throw( Unauthorized, "Unauthorized" )
        }
    }

    init {
        install( Unauthorized => nullProcess )
    }

    main{
        [getOrder(request)(response){
           validateToken
           response.id = request.id
           response.ammount = global.orders.(request.id).ammount
        }]
        [getOrders(request)(response){
            validateToken
            foreach (orderId : global.orders ){
                response.orders[#response.orders]<< {id = orderId
                                                     ammount = global.orders.(orderId).ammount }
            }
        }]
        [addOrder(request)(){
            validateToken
            getRandomUUID@stringUtils(  )( orderId )
            global.orders.(orderId).ammount = request.ammount
            locationHeader.Location = "/api/orders/" + orderId
        }]
    }
}