interface JabukaInterface {
RequestResponse:
	shutdown(void)(void),
	getKeyboardBrightness(void)(int),
	setKeyboardBrightness(int)(void),
	keyboardBrightnessUp(void)(void),
	keyboardBrightnessDown(void)(void),
	screenBrightnessUp(void)(void),
	screenBrightnessDown(void)(void)
}