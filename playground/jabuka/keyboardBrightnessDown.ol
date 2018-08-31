include "JabukaClient.iol"

main
{
	getKeyboardBrightness@Jabuka()( level );
	setKeyboardBrightness@Jabuka( level - 10 )()
}