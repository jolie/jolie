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

			// usage of partials online
			render = render@mustache( {
					template = "<h1>{{title}}</h1>{{> hello }}"
					partials << {
						name = "hello"
						template = "<p>Hello {{name}}</p>"
					}
					data << {
						title = "MyTitle"
						name = "Homer"
					}
			})
			if( render != "<h1>MyTitle</h1><p>Hello Homer</p>" ) throw( TestFailed, "online partials 1. found " + render )

			if(
				render@mustache( {
					template = "<h1>{{title}}</h1>{{> hello }}<hr>{{#items}}{{> item_list}}{{/items}}"
					partials[0] << {
						name = "hello"
						template = "<p>Hello {{name}}</p>"
					}
					partials[1] << {
						name = "item_list"
						template = "<p>Item {{item}}</p>"
					}
					data << {
						title = "MyTitle"
						name = "Homer"
						items[0].item = "1"
						items[1].item = "2"
						items[2].item = "3"
					}
				} )
				!= "<h1>MyTitle</h1><p>Hello Homer</p><hr><p>Item 1</p><p>Item 2</p><p>Item 3</p>"
			) throw( TestFailed, "online partials 2" )

			render = render@mustache( {
					template = "start {{#b}}{{> recursive}}{{/b}}"
					partials[0] << {
						name = "recursive"
						template = "1{{#a}}{{> recursive}}{{/a}}{{a}}"
					}
					data << {
						b.a.a.a.a = 0
					}
				} )
			if( render != "start 111110" ) throw( TestFailed, "online partials 3, found " + render )
			
		}
	}
}
