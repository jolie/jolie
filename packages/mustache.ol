/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2024 Claudio Guidi <cguidi@italianasoftware.com>
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

type RenderRequest {
	template:string ///< The mustache template
	data:undefined ///< The data for the template
	dir?:string ///< The directory in which to look for other templates (for partials)
} | void {
	template:string ///< The mustache template
	data:undefined ///< The data for the template
	partials* { ///< List of other templates to be used
		name: string 
		template: string
	}
}

interface MustacheInterface {
RequestResponse:
	/// Renders a mustache template
	render( RenderRequest )( string )
}

service Mustache {
	inputPort Input {
		location: "local"
		interfaces: MustacheInterface
	}

	foreign java {
		class: "joliex.mustache.MustacheService"
	}
}