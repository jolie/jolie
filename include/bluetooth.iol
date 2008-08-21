outputPort Bluetooth {
OneWay:
	setDiscoverable
RequestResponse:
	inquire, discoveryServices
}

embedded {
Java:
	"joliex.net.BluetoothService" in Bluetooth
}
