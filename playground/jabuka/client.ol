include "console.iol"
include "JabukaClient.iol"

main
{
	getKeyboardBrightness@Jabuka()( level );
	println@Console( level )()
}