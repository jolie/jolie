type DateValuesType:void {
	.day:int
	.month:int
	.year:int
}

/**
	Request type for operation getDateValues. 
	The root value contains the string to be converted into a DateValuesType
*/
type DateValuesRequestType:string {
	/**
		Date format.
		If not specified, it defaults to "dd/MM/yyyy"
	*/
	.format?:string
}


outputPort Time {
OneWay:
	setNextTimeout, setNextTimeoutByDateTime, setNextTimeoutByTime
RequestResponse:
	getCurrentDateTime, sleep,

	/**
		Converts an input string into a date expressed by means of
		three elements: day, month and year. The request may specify the 
		date parsing format. See #DateValuesRequestType for details.
	*/
	getDateValues(DateValuesRequestType)(DateValuesType),

	/**
		Returns the current date splitted in three fields: day, month and year
	*/
	getCurrentDateValues(void)(DateValuesType)
}

embedded {
Java:
	"joliex.util.TimeService" in Time
}
