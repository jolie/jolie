include "storage.iol"

type ConnectRequest:void {
	.filename:string
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

