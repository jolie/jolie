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

include "ThermostatInterface.iol"

execution{ single }

inputPort Thermostat {
    Location: MQTT_ServerLocation
    Protocol: mqtt {
        .debug = false;
        .broker = MQTT_BrokerLocation;
        .osc.getTmp << {
            .format = "raw",
            .alias = "42/getTemperature",
            .QoS = 2
        };
        .osc.setTmp << {
            .format = "raw",
            .alias = "42/setTemperature",
            .QoS = 2
        }
    }
    Interfaces: ThermostatInterface
}

main {
    [ getTmp( request )( response ) { 
        response = 19
    } ]
    |
    [ setTmp( request ) ]
}