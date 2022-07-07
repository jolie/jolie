constants
{
	JOLIE_HOME = "JOLIE_HOME",
	DIST_FOLDER = "dist",
	JOLIE_FOLDER = "jolie"
}

type ErrorType { message: string }

interface InstInterface {
RequestResponse:
	getDJH( void )( string ),
	getDLP( void )( string ),
	copyBins( string )( void ) throws CannotCopyBins( ErrorType ),
	copyLaunchers( string )( void ) throws CannotCopyInstallers( ErrorType ),
	mkdir( string )( void ),
	deleteDir( string )( void ),
	normalisePath( string )( string ),
	installationFinished( string )( void )
}