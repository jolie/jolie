outputPort Time {
Notification:
	setNextTimeout, setNextTimeoutByDateTime, setNextTimeoutByTime
SolicitResponse:
	getCurrentDateTime, sleep
}

embedded {
Java:
	"joliex.util.TimeService" in Time
}
