package jolie.lang.parse.module;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FinderCreator
{

    private final Path workingDirectory;
    private final Path[] packagesDirectories;

    public FinderCreator( String[] packagesDirectoriesStr )
            throws FileNotFoundException
    {
        this.workingDirectory = Paths.get("");
        this.packagesDirectories = Arrays.stream(packagesDirectoriesStr)
                .map( ( packageDirectoryStr ) -> Paths.get( packageDirectoryStr ) )
                .toArray( Path[]::new );
    }

    public FinderCreator( Path workingDirectory, String[] packagesDirectoriesStr )
            throws FileNotFoundException
    {
        this.workingDirectory = workingDirectory;
        this.packagesDirectories = Arrays.stream(packagesDirectoriesStr)
                .map( ( packageDirectoryStr ) -> Paths.get( packageDirectoryStr ) )
                .toArray( Path[]::new );
    }

    /**
     * returns a Finder object corresponding to target either it is a relative import(starts with .)
     * or absolute one
     */
    public Finder getFinderForTarget( URI source, String[] target )
    {
        final boolean isRelativeImport = target[0].isEmpty() ? true : false;

        if ( isRelativeImport ) {
            return new RelativePathFinder( target, source );
        } else {
            return new AbsolutePathFinder( target, this.workingDirectory,
                    this.packagesDirectories );
        }
    }
}
