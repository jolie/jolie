type Person:void {
	.firstName:string
	.lastName:string
}

type StartMessage:void {
	.clientLocation:any
	.person:Person
}

type OnSessionEndMessage:void {
	.sid:int
	.person:Person
}

interface ServerInterface {
OneWay:
	endSession(Person)
RequestResponse:
	startSession(StartMessage)(int)
}

interface ClientInterface {
OneWay:
	onSessionEnd(OnSessionEndMessage)
}


