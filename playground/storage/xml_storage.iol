include "storage.iol"

type ConnectRequest:void {
	.filename:string
	.charset?:string // set the encoding. Default: system (eg. for Unix-like OS UTF-8) or header specification
}

interface XmlStorageInterface {
RequestResponse:
	connect(ConnectRequest)(void) throws StorageFault
}

outputPort XmlStorage {
Interfaces: StorageInterface, XmlStorageInterface
}

embedded {
Java:
	"joliex.storage.XmlStorage" in XmlStorage
}

