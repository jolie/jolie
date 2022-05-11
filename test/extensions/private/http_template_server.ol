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
           .compression= false
           .osc.getOrder.template = "/api/orders/{id}"
           .osc.getOrder.method = "GET"
           .osc.getOrder.inHeaders.Authorization = "token"
           .osc.getOrders.template = "/api/orders"
           .osc.getOrders.method = "GET"
           .osc.getOrders.inHeaders.Authorization = "token"
           .osc.addOrder.template="/api/orders"
           .osc.addOrder.method="POST"
           .osc.addOrder.inHeaders.Authorization = "token"
        }
        location : "socket://localhost:9099"
    }
    execution: concurrent

    embed StringUtils as stringUtils
    embed Console as console

    main{
        [getOrder(request)(response){
           response.id = request.id
           response.ammount = global.orders.(request.id).ammount
        }]
        [getOrders(request)(response){
            foreach (orderId : global.orders ){
                response.orders[#response.orders]<< {id = orderId
                                                     ammount = global.orders.(orderId).ammount }
            }
        }]
        [addOrder(request)(response){
            getRandomUUID@stringUtils(  )( orderId )
            global.orders.(orderId).ammount = request.ammount

        }]
    }
}