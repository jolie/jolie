/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
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

from ..test-unit import TestUnitInterface
from mustache import Mustache

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed Mustache as mustache

	main {
		test()() {
			if(
				render@mustache( {
					template = ""
					data = {}
				} )
				!= ""
			) throw( TestFailed, "empty template" )

			if(
				render@mustache( {
					template = "{{name}}"
					data << { name = "Homer" }
				} )
				!= "Homer"
			) throw( TestFailed, "name template" )

			if(
				render@mustache( {
					template = "{{#cond}}Don't{{/cond}}OK"
					data << { cond = false }
				} )
				!= "OK"
			) throw( TestFailed, "false section" )

			if(
				render@mustache( {
					template = "{{#cond}}true{{/cond}}{{^cond}}false{{/cond}}"
					data << { cond = false }
				} )
				!= "false"
			) throw( TestFailed, "inverted section" )

			if(
				render@mustache( {
					template = "{{#items}}{{x}}{{/items}}"
					data << {
						items[0].x = 0
						items[1].x = 1
					}
				} )
				!= "01"
			) throw( TestFailed, "array" )

			if(
				render@mustache( {
					template = "{{#address}}{{street}}, {{city}}{{/address}}"
					data << {
						address << {
							street = "Supervej"
							city = "Odense"
						}
					}
				} )
				!= "Supervej, Odense"
			) throw( TestFailed, "structure" )
		}
	}
}
