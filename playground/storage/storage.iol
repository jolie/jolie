type Node:void {
	.name:string
	.index?:int
}

type Path:void {
	.node[0,*]: Node
}

type SaveRequest:void {
	.path:Path
	.value:undefined
}

type LoadRequest:Path

interface StorageInterface {
RequestResponse:
	load(LoadRequest)(undefined) throws StorageFault(string),
	save(SaveRequest)(void) throws StorageFault(string)
}

