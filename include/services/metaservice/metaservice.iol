/***************************************************************************
 *   Copyright (C) 2008-2009 by Fabrizio Montesi                           *
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

type MetaData:void { ? }

type ServiceRecord:void {
	.isEmbedded:int
	.resourceName:string
	.metadata?:MetaData
}

type ServiceList:void {
	.service[0,*]:ServiceRecord
}

/**
 * MetaService consultation interface.
 * Allows the retrieval of information about running services.
 */
interface MetaServiceConsultation {
RequestResponse:
	/**
	 * Returns a list of the available services, both internal or external.
	 * @response:void {
	 * 	.service[*]:void {
	 * 		.resourceName:string the resource this service is published under.
	 * 		.metadata:? the metadata associated to this service.
	 * 	}
	 * }
	 */
	getServices(void)(ServiceList)
}

type LoadEmbeddedJolieServiceRequest:void {
	.resourcePrefix:string
	.filepath:string
	.metadata?:MetaData
	.exposedProtocol?:string
}

type AddRedirectionRequest:void {
	.resourcePrefix:string
	.location:string
	.protocol:string { ? }
	.metadata?:MetaData
	.token?:int
	.exposedProtocol?:string
}

/**
 * MetaService administration interface.
 */
interface MetaServiceAdministration {
OneWay:
	/**
	 * Shuts down MetaService.
	 */
	shutdown(void)
RequestResponse:
	/**
	 * Starts an embedded jolie service reading its source code file,
	 * publishes it as a resource and returns the created resource name.
	 * @request:void {
	 * 	.resourcePrefix:string
	 * 		the first part of the resource name
	 * 		the embedded jolie service will be published under,
	 * 		e.g. if resourcePrefix="MediaPlayer" then the service
	 * 		will be published in /MediaPlayer or in /MediaPlayer-s, where s is a string.
	 * 	.filepath:string
	 * 		the source file path of the jolie service to embed.
	 * 	.metadata:void:?
	 * 		custom metadata. The content is intended to be used
	 * 		by other applications, not by MetaService itself.
	 * }
	 * @response:string the resource name the service has been published under
	 * @throws EmbeddingFault if the service could not be embedded
	 */
	loadEmbeddedJolieService(LoadEmbeddedJolieServiceRequest)(string) throws EmbeddingFault,
	/**
	 * Stops an embedded jolie service running under the specified resource.
	 * @request:string the resource name of the service to stop.
	 */
	unloadEmbeddedService(string)(void),
	/**
	 * Adds a redirection.
	 * @request:void {
	 * 	.resourcePrefix:string
	 * 		the first part of the resource name
	 * 		the redirection will be published under,
	 * 		e.g. if resourceName="MediaPlayer" then the redirection
	 * 		will be published in /MediaPlayer or in /MediaPlayer-s, where s is a string.
	 * 	.location:string the location (in JOLIE format) the redirection has to point to.
	 * 	.protocol:? the protocol (in JOLIE format) the redirection has to use.
	 * }
	 */
	addRedirection(AddRedirectionRequest)(string),
	/**
	 * Removes an existing redirection.
	 * @request:string the resource name identifying the redirection to remove.
	 */
	removeRedirection(string)(void)
}
