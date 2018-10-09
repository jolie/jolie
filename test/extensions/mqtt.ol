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
include "private/ThermostatInterface.iol"

outputPort Server {
    Location: MQTT_BrokerLocation
    Protocol: mqtt {
        .debug = false;
        .osc.getTmp << {
            .format = "raw",
            .alias = "%!{id}/getTemperature",
            .QoS = 2
        };
        .osc.test << {
            .format = "raw",
            .alias = "test/getTemperature",
            .QoS = 2
        }
    }
    Interfaces: ThermostatInterface
}

embedded {
Jolie:
    "private/mqtt_server.ol"
}

define doTest
{
    getTmp@Server( { .id = "42" } )( response );
    println@Console( "\nThermostat n.42 forwarded temperature " + response + " C" )();
    
    t_confort = 21;
    if (response < t_confort) {
        setTmp@Server( 21 { .id = "42" } )
        |
        println@Console( "\nSet Temperature of Thermostat n.42 to 21 C" )()
    }
}