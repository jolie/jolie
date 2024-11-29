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
	data?:undefined ///< The data for the template
	dir?:string ///< The directory in which to look for other templates (for partials)
	recursionLimit?: int ///< Maximum limit for recursive calls in partials (default is 100)
	partialsRecursionLimit?: int ///< Maximum limit for recursive calls specific for partials (default is 10)
	functions? { ///< A service that offers functions to templates
		name?: string ///< The name under which the functions can be accessed by the template (default is "fn")
		binding { ///< The binding information to reach the function provider
			location: any ///< Only locations as returned by getLocalLocation@runtime are currently supported
		}
	}
} | {
	template:string ///< The mustache template
	data?:undefined ///< The data for the template
	partials* { ///< List of other templates to be used
		name: string 
		template: string
	}
	recursionLimit?: int ///< Maximum limit for recursive calls in partials. Default is 100
	partialsRecursionLimit?: int ///< Maximum limit for recursive calls specific for partials. Default is 10
	functions? { ///< A service that offers functions to templates
		name?: string ///< The name under which the functions can be accessed by the template (default is "fn"). Be careful in not choosing a name that is already present in `data`, since this would be overriden.
		binding { ///< The binding information to reach the function provider
			location: any ///< Only locations as returned by getLocalLocation@runtime are currently supported
		}
	}
}

interface MustacheInterface {
RequestResponse:
	/// Renders a mustache template
	render( RenderRequest )( string )
}

/// Request to invoke a function in a mustache template
type MustacheInvocationRequest {
	name: string ///< The name of the function being called
	template?: string ///< The template to pass to the function as argument, if relevant
}

/// Response to a function invocation in a mustache template
type MustacheInvocationResponse: string | undefined

interface MustacheFunctionProviderInterface {
RequestResponse:
	call( MustacheInvocationRequest )( MustacheInvocationResponse )
}

service Mustache {
	inputPort Input {
		location: "local"
		interfaces: MustacheInterface
	}

	outputPort MustacheFunctionProvider {
		interfaces: MustacheFunctionProviderInterface
	}

	foreign java {
		class: "joliex.mustache.MustacheService"
	}
}