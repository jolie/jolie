/*
 *   Copyright (C) 2008-2021 by Fabrizio Montesi <famontesi@gmail.com>          
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

from types.JavaException import JavaExceptionType, WeakJavaExceptionType
from types.IOException import IOExceptionType

type FileNotFoundType:WeakJavaExceptionType

/**!
from: the source directory to copy
to: the target directory to copy into
*/
type CopyDirRequest: void {
	.from: string
	.to: string
}

type ReadFileRequest {
	filename:string
	format?:string { // "text" (default), "base64" (same as "binary" but afterwards base64-encoded), "binary", "xml" (a type-annotated XML format), "xml_store", "properties" (Java properties file) or "json"
		charset?:string // set the encoding. Default: system (eg. for Unix-like OS UTF-8), header specification (XML) or format's default (for XML and JSON UTF-8)
		skipMixedText?: bool // in case of format xml, it skips the mixed elements
		stream?:bool //< if format is "yaml" and this is true, the file is read as a stream of multiple YAML documents which will be returned as a "documents" array in the response
	}
}

type WriteFileRequest:void {
	.filename:string
	.content:undefined
	.format?:string { // "text", "binary", "xml", "xml_store" (a type-annotated XML format) or "json" (defaults to "binary" if contents' base value is raw, "text" otherwise)
		.doctype_system?:string // If format is "xml", adds it as a DOCTYPE system tag
		.schema*:string
		.indent?:bool // if true, indentation is applied to file (default: false)
		.encoding?:string // set the encoding. Default: system (eg. for Unix-like OS UTF-8) or format's default (for XML and JSON UTF-8)
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
	.regex?:string
	.recursive?:bool
	.dirsOnly?:bool	// List only directories?
	.order?: void {
		.byname?: bool
	}
	.info?: bool // it returns also file infos. Default is false
}

type ListResponse:void {
	.result[0,*]:string {
		.info?: void {
			.lastModified: long
			.size: long
			.absolutePath: string
			.isHidden: bool
			.isDirectory: bool
		}
	}
}

interface FileInterface {
RequestResponse:
	/**!
	 * Constructs an absolute path to the target file or directory.
	 * Can be used to construct an absolute path for new files that does not exist yet.
	 * Throws a InvalidPathException fault if input is a relative path is not system recognized path.
	 */
	toAbsolutePath( string )( string ) throws InvalidPathException( JavaExceptionType ),

	/**!
	 * Constructs the path to the parent directory.
	 * Can be used to construct paths that does not exist so long as the path uses the system's filesystem path conventions.
	 * Throws a InvalidPathException fault if input path is not a recognized system path or if the parent has no parent.
	 */
	getParentPath( string )( string ) throws InvalidPathException( JavaExceptionType ),

	/**!
	  it returns if a filename is a directory or not. False if the file does not exist.
	*/
	isDirectory( string )( bool ) throws FileNotFound(FileNotFoundType) IOException(IOExceptionType),

	/**!
	 * Reads some file's content into a Jolie structure
	 *
	 * Supported formats (ReadFileRequest.format):
	 * - text (the default)
	 * - base64 (same as binary but afterwards base64-encoded)
	 * - binary
	 * - xml
	 * - xml_store (a type-annotated XML format)
	 * - properties (Java properties file)
	 * - json
	 *
	 * Child values: text, base64 and binary only populate the return's base value, the other formats fill in the child values as well.
	 * - xml, xml_store: the XML root node will costitute a return's child value, the rest is filled in recursively
	 * - properties: each property is represented by a child value
	 * - json: each attribute corresponds to a child value, the default values (attribute "$" or singular value) are saved as the base values, nested arrays get mapped with the "_" helper childs (e.g. a[i][j] -> a._[i]._[j]), the rest is filled in recursively
	 */
	readFile(ReadFileRequest)(undefined)
		throws FileNotFound(FileNotFoundType) IOException(IOExceptionType),

	/**!
	 * Writes a Jolie structure out to an external file
	 *
	 * Supported formats (WriteFileRequest.format):
	 * - text (the default if base value not of type raw)
	 * - binary (the default if base value of type raw)
	 * - xml
	 * - xml_store (a type-annotated XML format)
	 * - json
	 *
	 *
	 * Child values: text and binary only consider the content's (WriteFileRequest.content) base value, the other formats look at the child values as well.
	 * - xml, xml_store: the XML root node will costitute the content's only child value, the rest gets read out recursively
	 * - json: each child value corresponds to an attribute, the base values are saved as the default values (attribute "$" or singular value), the "_" helper childs disappear (e.g. a._[i]._[j] -> a[i][j]), the rest gets read out recursively
	 *
	 *	when format is xml and a schema is defined, the resulting xml follows the schema constraints.
	 *  Use "@NameSpace" in order to enable root element identification in the schema by specifing the namespace of the root.
	 *  Use "@Prefix" for forcing a prefix in an element.
	 *  Use "@ForceAttribute" for forcing an attribute in an element even if it is not defined in the corresponding schema
	 */
	writeFile(WriteFileRequest)(void) throws FileNotFound(FileNotFoundType) IOException(IOExceptionType),

	/**!
	  it copies a source directory into a destination one
	*/
	copyDir( CopyDirRequest )( bool ) throws IOException FileNotFound,

	delete(DeleteRequest)(bool) throws IOException(IOExceptionType),

	/**!
	   it deletes a directory recursively removing all its contents
	*/
	deleteDir( string )( bool ) throws IOException(IOExceptionType),

	/**!
	 * The size of any basic type variable.
	 * - raw: buffer size
	 * - void: 0
	 * - boolean: 1
	 * - integer types: int 4, long 8
	 * - double: 8
	 * - string: size in the respective platform encoding, on ASCII and latin1
	 *   equal to the string's length, on Unicode (UTF-8 etc.) >= string's length
	 */
	getSize( any )( int ),

	rename(RenameRequest)(void) throws IOException(IOExceptionType),
	list(ListRequest)(ListResponse) throws IOException(IOExceptionType),
	/**!
	*
	* it creates the directory specified in the request root. Returns true if the directory has been
	* created with success, false otherwise
	*/
	mkdir( string )( bool ),

	/**!
	* it tests if the specified file or directory exists or not.
	*/
	exists( string )( bool ),

	/** Returns the parent path of the service */
	getServiceParentPath(void)(string),

	/** Returns the filesystem directory from which the service has been launched */
	getServiceDirectory(void)(string) throws IOException(IOExceptionType),
	/** Returns the name of the file from which the service has been launched */
	getServiceFileName(void)(string),
	/** Returns the real filesystem directory (following links) from which the service has been launched */
	getRealServiceDirectory(void)(string) throws IOException(IOExceptionType),
	/** Returns the name of the real file (following links) from which the service has been launched */
	getRealServiceFileName(void)(string) throws IOException(IOExceptionType),
	getFileSeparator(void)(string),
	getMimeType(string)(string) throws FileNotFound(FileNotFoundType),
	setMimeTypeFile(string)(void) throws IOException(IOExceptionType),

	/**! deprecated, please use rawToBase64@Converter()() from converter.iol */
	convertFromBinaryToBase64Value( raw )( string ),
	/**! deprecated, please use base64ToRaw@Converter()() from converter.iol */
	convertFromBase64ToBinaryValue( string )( raw ) throws IOException(IOExceptionType)

}

service File {
    inputPort ip {
        location:"local"
        interfaces: FileInterface
    }

    foreign java {
        class: "joliex.io.FileService"
    }
}