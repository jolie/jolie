/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
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


include "types/JavaException.iol"

type SendMailRequest:void {
	.host:string
	.authenticate?:void {
		.username:string
		.password:string
	}
	.from:string
	.to[1,*]:string
	.cc[0,*]:string
	.bcc[0,*]:string
	.subject:string
	.content:string
}

interface SMTPInterface {
RequestResponse:
	sendMail(SendMailRequest)(void) throws SMTPFault(JavaExceptionType)
}

outputPort SMTP {
Interfaces: SMTPInterface
}

embedded {
Java:
	"joliex.mail.SMTPService" in SMTP
}

