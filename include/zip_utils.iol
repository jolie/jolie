/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

include "types/IOException.iol"

type ReadEntryRequest:void {
	.filename:string
	.entry:string
}

type ZipRequest:void { ? }

type UnzipRequest: void {
	.filename: string
	.targetPath: string
}

type UnzipResponse: void {
	.entry*: string
}

interface ZipUtilsInterface {
RequestResponse:
	readEntry(ReadEntryRequest)(any) throws IOException(IOExceptionType),
	zip(ZipRequest)(raw) throws IOException(IOExceptionType),
	unzip( UnzipRequest )( UnzipResponse ) throws FileNotFound, IOException
}

outputPort ZipUtils {
Interfaces: ZipUtilsInterface
}

embedded {
Java:
	"joliex.util.ZipUtils" in ZipUtils
}
