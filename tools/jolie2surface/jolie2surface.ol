from metajolie import MetaJolie
from metarender-code import MetaRenderCode
from console import Console
from file import File

service Jolie2Surface {

    embed MetaJolie as MetaJolie
    embed MetaRenderCode as MetaRenderCode
    embed Console as Console
    embed File as File

    main {
        if ( #args != 2 ) {
            println@Console("Usage jolie2surface <filename> <input>")()
        } else {
            filename = args[ 0 ]
            if ( !exists@File( filename ) ) { println@Console("Filename " + filename + " does not exist")() }
            else {
                input = args[ 1 ]
                getInputPortMetaData@MetaJolie({
                    filename = args[0]
                })( meta )

                found = false
                for ( ip in meta.input ) {
                    if ( ip.name == input ) {
                        getSurface@MetaRenderCode( ip )( surface )
                        println@Console( surface )()
                        found = true
                    }
                } 
                if ( !found ) { println@Console( "Input port " + input + " not found" )() }
            }
        }

    }
}