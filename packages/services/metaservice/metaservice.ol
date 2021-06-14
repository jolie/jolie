/*
 *   Copyright (C) 2008-2009 by Fabrizio Montesi                          
 *                                                                        
 *   This program is free software; you can redistribute it and/or modify 
 *   it under the terms of the GNU Library General Public License as      
 *   published by the Free Software Foundation; either version 2 of the   
 *   License, or (at your option) any later version.                      
 *                                                                        
 *   This program is distributed in the hope that it will be useful,      
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of       
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 *   GNU General Public License for more details.                         
 *                                                                        
 *   You should have received a copy of the GNU Library General Public    
 *   License along with this program; if not, write to the                
 *   Free Software Foundation, Inc.,                                      
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            
 *                                                                        
 *   For details about the authors of this software, see the AUTHORS file.
 */

from  console import Console
from  runtime import Runtime 
from .metaserviceInterface import MetaServiceConsultation
from .metaserviceInterface import MetaServiceAdministration


execution { sequential }


constants {
	Debug = 0,
	MetaServiceLocation = "socket://localhost:9000/",
	MetaServiceProtocol = sodep,
	//MetaServiceSOAPLocation = "socket://localhost:9001"
	MetaServiceSOAPLocation = "local"
}


service MetaService{



    embed Console as console
	embed Runtime as runtime

	inputPort MetaService {
		Location: MetaServiceLocation
		Protocol: MetaServiceProtocol
		Interfaces: MetaServiceConsultation, MetaServiceAdministration
	}

	inputPort MetaServiceSOAP {
		Location: MetaServiceSOAPLocation
		Protocol: soap { .interpretResource = 1 }
		Interfaces: MetaServiceConsultation, MetaServiceAdministration
	}

	outputPort ConfirmingService {
	RequestResponse:
		confirmRedirection(int)(int) throws InvalidToken
	}
    
	execution { sequential }


	init
	{
		install( TypeMismatch => println@console( "TypeMismatch: " + main.TypeMismatch )() );

		services -> global.services;

		config.debug = Debug;

		// TODO: implement a proper Logger service interface.
		if ( config.debug ) {
			install(
				EmbeddingFault => println@console( "Embedding fault thrown" )(),
				RedirectionNotFound => println@console( "RedirectionNotFound fault thrown" )()
			)
		} else {
			install(
				EmbeddingFault => nullProcess,
				RedirectionNotFound => nullProcess
			)
		}
	}

	main
	{
		[ addRedirection( request )( response ) {
			scope( addRedirectionScope ) {
				//install( RuntimeException => nullProcess ); // TODO: add a proper logging action here
				with( redirection ) {
					.resourceName = request.resourcePrefix;
					if ( request.exposedProtocol == "soap" ) {
						.inputPortName = "MetaServiceSOAP"
					} else {
						.inputPortName = "MetaService"
					}
				};
				getRedirection@runtime( redirection )( outputPortName );
				if ( is_defined( outputPortName ) ) {
					// If we already have that redirection, generate a fresh name for the new service.
					if ( is_defined( global.counter.(outputPortName) ) ) {
						global.counter.(outputPortName)++
					} else {
						global.counter.(outputPortName) = 2
					};
					redirection.resourceName = outputPortName + "-" + global.counter.(outputPortName)
				};

				with( port ) {
					.name = redirection.resourceName;
					.location = request.location;
					.protocol << request.protocol
				};
				setOutputPort@runtime( port )();

				// If we can't set the redirection, we must remove the output port.
				install( RuntimeException => removeOutputPort@runtime( port.name )(); cH );
				redirection.outputPortName = redirection.resourceName;
				setRedirection@runtime( redirection )();
				response = redirection.resourceName;
				
				with( services.(redirection.resourceName) ) {
					.isEmbedded = 0;
					if ( is_defined( request.metadata ) ) {
						.metadata[0] << request.metadata[0]
					}
				};

				if ( is_defined( request.token ) ) {
					// Optional token for security check
					ConfirmingService.location = request.location;
					ConfirmingService.protocol << request.protocol;
					install( InvalidToken => throw( RuntimeException ) );
					confirmRedirection@ConfirmingService( request.token )( confirmation );
					if ( confirmation != 1 ) {
						throw( RuntimeException )
					}
				}
			}
		} ] { nullProcess }

		[ removeRedirection( request )() {
			if ( is_defined( services.(request) ) && services.(request).isEmbedded == 0 ) {
				r.resourceName = request;
				r.inputPortName = "MetaService";
				removeRedirection@runtime( r )();
				removeOutputPort@runtime( request )();
				undef( services.(request) )
			}
		} ] { nullProcess }

		[ getServices()( response ) {
			i = 0;
			foreach( s : services ) {
				response.service[i].isEmbedded = services.(s).isEmbedded;
				if ( is_defined( services.(s).metadata[0] ) ) {
					response.service[i].metadata[0] << services.(s).metadata[0]
				};
				response.service[i].resourceName = s;
				i++
			}
		} ] { nullProcess }

		[ loadEmbeddedJolieService( request )( response ) {
			scope( embedJolieScope ) {
				install( RuntimeException => throw( EmbeddingFault ) );
				with( redirection ) {
					.resourceName = request.resourcePrefix;
					if ( request.exposedProtocol == "soap" ) {
						.inputPortName = "MetaServiceSOAP"
					} else {
						.inputPortName = "MetaService"
					}
				};
				getRedirection@runtime( redirection )( outputPortName );
				if ( is_defined( outputPortName ) ) {
					// If we already have that redirection, generate a fresh name for the new service.
					if ( is_defined( global.counter.(outputPortName) ) ) {
						global.counter.(outputPortName)++
					} else {
						global.counter.(outputPortName) = 2
					};
					redirection.resourceName = outputPortName + "-" + global.counter.(outputPortName)
				};

				// Load the embedded service.
				with( embedInfo ) {
					.type = "Jolie";
					.filepath = request.filepath
				};
				loadEmbeddedService@runtime( embedInfo )( handle );

				with( port ) {
					.name = redirection.resourceName;
					.location = handle
				};
				setOutputPort@runtime( port )();

				// If we can't set the redirection, we must also remove the output port.
				install( RuntimeException => removeOutputPort@runtime( port.name )(); cH );
				redirection.outputPortName = redirection.resourceName;
				setRedirection@runtime( redirection )();

				response = redirection.resourceName;

				with( services.(redirection.resourceName) ) {
					.isEmbedded = 1;
					.privates.handle = handle;
					if ( is_defined( request.metadata ) ) {
						.metadata[0] << request.metadata[0]
					}
				}
			}
		} ] { nullProcess }

		[ unloadEmbeddedService( request )( response ) {
			if ( is_defined( services.(request) ) ) {
				service -> services.(request);
				if ( service.isEmbedded ) {
					r.resourceName = request;
					r.inputPortName = "MetaService";
					removeRedirection@runtime( r )();
					removeOutputPort@runtime( request )();
					callExit@runtime( service.privates.handle )();
					undef( service );
					undef( services.(request) )
				}
			}
		} ] { nullProcess }

		[ shutdown() ] { exit }
	}
}
