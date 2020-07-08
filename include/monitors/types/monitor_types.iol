type LogEventData: void {
	level: string
	message: string 
	processId: string
	extendedType?: string
}

type OperationCallReplyEventData: void {
	operationName: string
	processId: string 
	messageId: string 
	status: int 
	details: string 
	outputPort: string 
	value: undefined
}

type OperationEndedEventData: void {
	operationName: string
	processId: string 
	messageId: string 
	status: int 
	details: string 
	value: undefined
}

type OperationStartedEventData: void {
	operationName: string
	processId: string 
	messageId: string 
	value: undefined
}

type ProtocolMessageEventData: void {
	protocol: string 
	header: string 
	message: string
	processId: string  
}

type SessionEventData: void {
	operationName: string
	processId: string 
}

type MonitorAttachedEventData: void


type MonitorEvent: void {
	type: string 
	memory: long
	timestamp: long
	service: string
	cellId: int
	scope: string
	data?: LogEventData 
		| OperationCallReplyEventData 
		| OperationEndedEventData 
		| OperationStartedEventData
		| ProtocolMessageEventData
		| SessionEventData
		| MonitorAttachedEventData
	context?: void {
		filename: string 
		line: int
	}
}
