include "console.iol"
include "file.iol"
include "string_utils.iol"

include "config.iol"
include "JabukaInterface.iol"

execution { sequential }

inputPort JabukaInput {
Location: Jabuka_Location
Protocol: Jabuka_Protocol
Interfaces: JabukaInterface
}

inputPort JabukaHttpInput {
Location: Jabuka_HttpLocation
Protocol: http { .format = "html"; .method = "get" }
Interfaces: JabukaInterface
}

init
{
	f.filename = Jabuka_KeyboardMaxBrightnessFilename;
	readFile@File( f )( global.keyboard.maxBrightness );
	global.keyboard.maxBrightness = int( global.keyboard.maxBrightness );
	undef( f );
	f.filename = Jabuka_ScreenMaxBrightnessFilename;
	readFile@File( f )( global.screen.maxBrightness );
	global.screen.maxBrightness = int( global.screen.maxBrightness );
	undef( f )
}

init
{
	println@Console( "Jabuka started" )()
}

main
{
	[ shutdown()() { nullProcess } ] { exit }

	[ getKeyboardBrightness()( int(response) ) {
		f.filename = Jabuka_KeyboardBrightnessFilename;
		readFile@File( f )( response )
	} ] { nullProcess }

	[ setKeyboardBrightness( level )() {
		if ( level < 0 ) {
			level = 0
		} else if ( level > global.keyboard.maxBrightness ) {
			level = global.keyboard.maxBrightness
		};
		f.filename = Jabuka_KeyboardBrightnessFilename;
		f.content = string( level );
		writeFile@File( f )()
	} ] { nullProcess }

	[ keyboardBrightnessUp()() {
		f.filename = Jabuka_KeyboardBrightnessFilename;
		readFile@File( f )( level );
		level = int( level ) + Jabuka_KeyboardBrightnessStep;
		if ( level > global.keyboard.maxBrightness ) {
			level = global.keyboard.maxBrightness
		};
		f.content = string( level );
		writeFile@File( f )()
	} ] { nullProcess }

	[ keyboardBrightnessDown()() {
		f.filename = Jabuka_KeyboardBrightnessFilename;
		readFile@File( f )( level );
		level = int( level ) - Jabuka_KeyboardBrightnessStep;
		if ( level < 0 ) {
			level = 0
		};
		f.content = string( level );
		writeFile@File( f )()
	} ] { nullProcess }

	[ screenBrightnessUp()() {
		f.filename = Jabuka_ScreenBrightnessFilename;
		readFile@File( f )( level );
		level = int( level ) + Jabuka_ScreenBrightnessStep;
		if ( level > global.screen.maxBrightness ) {
			level = global.screen.maxBrightness
		};
		f.content = string( level );
		writeFile@File( f )()
	} ] { nullProcess }

	[ screenBrightnessDown()() {
		f.filename = Jabuka_ScreenBrightnessFilename;
		readFile@File( f )( level );
		level = int( level ) - Jabuka_ScreenBrightnessStep;
		if ( level < 0 ) {
			level = 0
		};
		f.content = string( level );
		writeFile@File( f )()
	} ] { nullProcess }
}