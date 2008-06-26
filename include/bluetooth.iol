outputPort Bluetooth {
Notification:
	setDiscoverable
SolicitResponse:
	inquire, discoveryServices
}

embedded {
Java:
	"joliex.net.BluetoothService" in Bluetooth
}
