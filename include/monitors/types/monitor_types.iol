type LogEventData: void {
	level: string
	message: string 
	extendedType?: string
}

type OperationCallReplyEventData: void {
	operationName: string
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
	messageId: string 
	value: undefined
}

type ProtocolMessageEventData: void {
	protocol: string 
	header: string 
	message: string 
}

type SessionEventData: void {
	operationName: string
}

type MonitorAttachedEventData: void

type ThrowEventData: void {
	faultname: string
}

type FaultHandlerStartedEventData: void {
	faultname: string
}

type FaultHandlerStartedEventData: void {
	faultname: string 
}

type MonitorEvent: void {
	type: string 
	memory: long
	timestamp: long
	service: string
	cellId: int
    processId: string 
	scope: string
	serialEventId: long
	data?: LogEventData 
		| OperationCallReplyEventData   //< covers also OperationCallEvent, OperationCallAsyncEvent and OperationReplyAsyncEvent
		| OperationEndedEventData 
		| OperationStartedEventData
		| ProtocolMessageEventData
		| SessionEventData
		| MonitorAttachedEventData
		| ThrowEventData 				//< covers also FaultHandlerStartedEvent and FaultHandlerEndedEvent
	context?: void {
		filename: string 
		line: int
	}
}
