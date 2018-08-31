/***************************************************************************
 *   Copyright (C) 2008-09-10 by Fabrizio Montesi <famontesi@gmail.com>    *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as               *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public             *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

include "types.iol"

type OpenDocumentMessage:void {
	.documentUrl:string
	.local?:int  // 1 if sent from a local Viewer, 0 or undefined otherwise
}

type CloseClientSessionMessage:int // The client session id

type StartClientSessionRequest:void {
	.location:string // The location of the client
}

type StartClientSessionResponse:void {
	.documentUrl:string
	.pageNumber:int
	.sid:int
}

interface PresenterInterface {
OneWay:
	goToPage(GoToPageMessage),
	openDocument(OpenDocumentMessage),
	closeClientSession(CloseClientSessionMessage)
RequestResponse:
	startClientSession(StartClientSessionRequest)(StartClientSessionResponse)
}

outputPort Presenter {
Protocol: sodep
Interfaces: PresenterInterface
}
