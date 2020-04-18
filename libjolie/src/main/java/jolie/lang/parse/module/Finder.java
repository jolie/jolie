package jolie.lang.parse.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finder find a module target, it is divided into two class, a finder for relative path or absolute
 * path
 * 
 * @return
 * @throws ModuleException if there is finder cannot locate any file.
 */
public abstract class Finder
{
    protected final String[] target;
    private final List< String > lookedPaths;

    protected Finder( String[] target )
    {
        this.target = target;
        this.lookedPaths = new ArrayList<>();
    }

    public String[] lookupedPath()
    {
        return lookedPaths.toArray( new String[0] );
    }

    protected int moduleIndex()
    {
        return target.length - 1;
    }

    /**
     * Find a module target, return a File object points to first found path.
     * 
     * @return
     * @throws ModuleException if there is finder cannot locate any file.
     */
    abstract Source find() throws ModuleException;

    /**
     * returns a Finder object corresponding to target either it is a relative import(starts with .)
     * or absolute one
     */
    static Finder getFinderForTarget( URI source, String[] includePathStrings, String[] target )
    {
        final boolean isRelativeImport = target[0].isEmpty() ? true : false;

        if ( isRelativeImport ) {
            return new RelativePathFinder( target, source );
        } else {
            return new AbsolutePathFinder( target, includePathStrings );
        }
    }

    /**
     * @return an array of tokens except last one, which denote the module name
     */
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, 0, this.target.length - 1 );
    }

    /**
     * @return an importing module name
     */
    protected String moduleName()
    {
        return this.target[this.target.length - 1];
    }

    /**
     * Perform a lookup to a directory name from basePath
     * 
     * @param basePath base path for lookup
     * @param dirName  directory name
     * @return a new path of directory, null if file is not found
     */
    protected Path directoryLookup( Path basePath, String dirName )
    {
        Path dirPath = basePath.resolve( dirName );
        if ( Files.exists( dirPath ) && Files.isDirectory( dirPath ) ) {
            return dirPath;
        }
        lookedPaths.add( dirPath.toString() );
        return null;
    }

    /**
     * Perform a lookup to a jap filename from basePath
     * 
     * @param basePath base path for lookup
     * @param filename a filename
     * @return a new File of jap file, null if file is not found
     */
    protected File japLookup( Path basePath, String filename )
    {
        Path japPath = basePath.resolve( filename + ".jap" );
        lookedPaths.add( japPath.toString() );
        if ( Files.exists( japPath ) ) {
            return japPath.toFile();
        }
        return null;
    }

    /**
     * Perform a lookup to a ol filename from basePath
     * 
     * @param basePath base path for lookup
     * @param filename a filename
     * @return a new path of ol file, null if file is not found
     */
    protected File olLookup( Path basePath, String filename )
    {
        Path olPath = basePath.resolve( filename + ".ol" );
        lookedPaths.add( olPath.toString() );
        if ( Files.exists( olPath ) ) {
            return olPath.toFile();
        }
        return null;
    }


    protected Path locatePackage( Path basePath )
    {
        Path ret = basePath;
        for (String pathString : this.packagesToken()) {
            ret = this.directoryLookup( basePath, pathString );
            if ( ret == null ) {
                return ret;
            }
            // @TODO handle jap file
            // ret = this.japLookup(basePath, pathString);
        }
        return ret;
    }


    protected File locateModule( Path basePath )
    {
        Path packagePath = this.locatePackage( basePath );
        if ( packagePath == null ) {
            return null;
        }
        return this.olLookup( packagePath, this.moduleName() );
    }
}


class RelativePathFinder extends Finder
{

    private final URI source;

    protected RelativePathFinder( String[] target, URI source )
    {
        super( target );
        assert source != null;
        this.source = source;
    }


    private Path resolveDotPrefix()
    {
        Path sourcePath = Paths.get( source );
        Path basePath = sourcePath;
        for (int i = 1; i < super.moduleIndex(); i++) {
            if ( target[i].isEmpty() ) {
                basePath = basePath.getParent();
            } else {
                break;
            }
        }
        return basePath;
    }

    @Override
    Source find() throws ModuleException
    {
        Path basePath = resolveDotPrefix();

        FileSource moduleFile = new FileSource( super.locateModule( basePath ) );
        // @TODO handle jap file
        return moduleFile;
    }

}


class AbsolutePathFinder extends Finder
{
    private final String[] includePathStrings;

    protected AbsolutePathFinder( String[] target, String[] includePathStrings )
    {
        super( target );
        this.includePathStrings = includePathStrings;
    }

    @Override
    Source find() throws ModuleException
    {
        for (String baseDir : this.includePathStrings) {
            Path basePath = Paths.get( baseDir );
            FileSource moduleFile = new FileSource( super.locateModule( basePath ) );

            if ( moduleFile != null ) {
                return moduleFile;
            }
        }
        return null;
    }

}


interface Source
{
    URI source();

    InputStream stream() throws FileNotFoundException, IOException;
}


class FileSource implements Source
{

    File file;

    public FileSource( File f )
    {
        this.file = f;
    }

    @Override
    public URI source()
    {
        return this.file.toURI();
    }

    @Override
    public InputStream stream()
    {
        try {
            return new FileInputStream( this.file );
        } catch (FileNotFoundException e) {
        }
        return null;
    }
}
