/*
 * Copyright (C) 2017 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

include "../AbstractTestUnit.iol"
include "console.iol"
include "private/thermostatService.iol"

outputPort Server {
    Location: CoAP_ServerLocation
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .alias = "/%!{id}/getTemperature",
            .messageCode = "GET",
            .messageType = "CON"
        };
        .osc.setTmp << {
            .contentFormat = "text/plain",
            .alias = "/%!{id}/setTemperature",
            .messageCode = "POST",
            .messageType = "CON"
        };
        .osc.core << {
            .alias = "/.well-known/core",
            .messageCode = "GET",
            .messageType = "CON"
        }
    }
    Interfaces: ThermostatInterface
}

embedded {
Jolie:
    "private/coap_server.ol"
}

define doTest
{
    println@Console( "Resources available @ Thermostat are:\n" )( ) | {
        core@Server( )( response );
        println@Console( response )()
    };

    getTmp@Server( { .id = "42" } )( response );
    println@Console( "\nThermostat n.42 forwarded temperature " + response + " C" )();
    
    t_confort = 21;
    if (response < t_confort) {
        setTmp@Server( 21 { .id = "42" } )
        |
        println@Console( "\nSet Temperature of Thermostat n.42 to 21 C" )()
    }
}
