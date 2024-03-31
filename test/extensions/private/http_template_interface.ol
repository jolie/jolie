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
type UnauthorizedException:void{
    Unauthorized:string
}

type Order:void{
    id:string
    ammount:double
}

type GetOrderRequest:void{
    token:string
    id:string
}

type GetOrderResponse:Order

type GetOrdersRequest:void{
    token:string
}

type GetOrdersResponse:void{
    orders*: Order
}

type AddOrderRequest:void{
    token:string
    ammount:double
}

type AddOrderResponse:undefined|UnauthorizedException // "undefined" for resp headers

interface HttpTemplateInterface{
    RequestResponse:

    getOrders(GetOrdersRequest)(GetOrdersResponse) throws Unauthorized,
    getOrder(GetOrderRequest)(GetOrderResponse) throws Unauthorized,
    addOrder(AddOrderRequest)(AddOrderResponse) throws Unauthorized,
    notExisting(void)(void) // invalid call
}
