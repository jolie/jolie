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


include "types/IOException.iol"

type ReadFileRequest:void {
	.filename:string
	.format?:string { // Can be "base64", "binary", "text" or "xml" (defaults to "text")
		.charset?:string
	}
}

type WriteFileRequest:void {
	.filename:string
	.content:undefined
	.format?:string { // Can be "binary", "text" or "xml" (defaults to "text")
		.schema*:string
	}
	.append?:int // Default: 0
}

type DeleteRequest:string { // The filename to delete
	.isRegex?:int // 1 if the filename is a regular expression, 0 otherwise
}

type RenameRequest:void {
	.filename:string
	.to:string
}

type ListRequest:void {
	.directory:string
	.regex:string
}

type ListResponse:void {
	.result[0,*]:string
}

interface FileInterface {
RequestResponse:
	readFile(ReadFileRequest)(undefined) throws FileNotFound(void) IOException(IOExceptionType),
	writeFile(WriteFileRequest)(void) throws FileNotFound(void) IOException(IOExceptionType),
	delete(DeleteRequest)(int) throws IOException(IOExceptionType),
	rename(RenameRequest)(void) throws IOException(IOExceptionType),
	list(ListRequest)(ListResponse),
	mkdir(string)(int),
	exists(string)(int),
	getServiceDirectory(void)(string),
	getFileSeparator(void)(string),
	getMimeType(string)(string) throws FileNotFound(void),
	setMimeTypeFile(string)(void) throws IOException(void)
}

outputPort File {
Interfaces: FileInterface
}

embedded {
Java:
	"joliex.io.FileService" in File
}
