outputPort Time {
OneWay:
	setNextTimeout, setNextTimeoutByDateTime, setNextTimeoutByTime
RequestResponse:
	getCurrentDateTime, sleep
}

embedded {
Java:
	"joliex.util.TimeService" in Time
}
