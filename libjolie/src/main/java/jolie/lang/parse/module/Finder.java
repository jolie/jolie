package jolie.lang.parse.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import jolie.lang.Constants;
import jolie.util.UriUtils;

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
    protected abstract String[] packagesToken();

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
            ret = this.directoryLookup( ret, pathString );
            if ( ret == null ) {
                return ret;
            }
            // @TODO handle jap file
            // ret = this.japLookup(basePath, pathString);
        }
        return ret;
    }


    protected Source locateModule( Path basePath ) throws ModuleException
    {
        Path packagePath = this.locatePackage( basePath );
        if ( packagePath == null ) {
            return null;
        }
        File moduleFile = this.olLookup( packagePath, this.moduleName() );
        if ( moduleFile != null ) {
            return new FileSource( moduleFile );
        }
        moduleFile = this.japLookup( packagePath, this.moduleName() );
        if ( moduleFile != null ) {
            try {
                return new JapSource( moduleFile );
            } catch (IOException e) {
                throw new ModuleException( "error jap file lookup", e );
            }
        }
        return null;
    }
}


class RelativePathFinder extends Finder
{

    private final URI source;
    private int packagesTokenStartIndex = 0;

    protected RelativePathFinder( String[] target, URI source )
    {
        super( target );
        assert source != null;
        this.source = source;
    }


    private Path resolveDotPrefix()
    {
        Path sourcePath = Paths.get( source );
        Path basePath = sourcePath.getParent();
        int i = 1;
        for (; i < super.moduleIndex(); i++) {
            if ( target[i].isEmpty() ) {
                basePath = basePath.getParent();
            } else {
                break;
            }
        }
        packagesTokenStartIndex = i;
        return basePath;
    }

    @Override
    Source find() throws ModuleException
    {
        Path basePath = resolveDotPrefix();
        Source moduleFile = super.locateModule( basePath );
        return moduleFile;
    }

    @Override
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, packagesTokenStartIndex, this.target.length - 1 );
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
            Source module = super.locateModule( basePath );
            if ( module != null ) {
                return module;
            }
        }
        return null;
    }

    @Override
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, 0, this.target.length - 1 );
    }

}


enum SourceType {
    FILE, JAP
}


interface Source
{
    URI source();

    SourceType type();

    InputStream stream() throws FileNotFoundException, IOException;
}


class FileSource implements Source
{

    final File file;

    public FileSource( File f )
    {
        this.file = f;
    }

    @Override
    public SourceType type()
    {
        return SourceType.FILE;
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


class JapSource implements Source
{

    final JarFile japFile;
    final URI source;
    final String filePath;
    final String parentPath;

    public JapSource( File f ) throws IOException
    {
        this.japFile = new JarFile( f );
        this.source = f.toURI();
        Manifest manifest = this.japFile.getManifest();

        if ( manifest != null ) { // See if a main program is defined through a Manifest attribute
            Attributes attrs = manifest.getMainAttributes();
            this.filePath = attrs.getValue( Constants.Manifest.MAIN_PROGRAM );
            this.parentPath = Paths.get( this.filePath ).getParent().toString();
        } else {
            throw new IOException( "unable to find main program for library " + f.getName() );
        }
    }

    public String includePath()
    {
        return "jap:" + this.source.toString() + "!/" + this.parentPath;
    }

    @Override
    public SourceType type()
    {
        return SourceType.JAP;
    }

    @Override
    public URI source()
    {
        return this.source;
    }

    @Override
    public InputStream stream()
    {
        try {
            ZipEntry z = japFile.getEntry( this.filePath );
            return this.japFile.getInputStream( z );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * copied from CommandLineParser, needed to be refactor
     */
    private static String parseJapManifestForMainProgram( Manifest manifest, JarFile japFile )
    {
        String filepath = null;
        if ( manifest != null ) { // See if a main program is defined through a Manifest attribute
            Attributes attrs = manifest.getMainAttributes();
            filepath = attrs.getValue( Constants.Manifest.MAIN_PROGRAM );
        }

        if ( filepath == null ) { // Main program not defined, we make <japName>.ol and
                                  // <japName>.olc guesses
            String name = new File( japFile.getName() ).getName();
            filepath =
                    new StringBuilder().append( name.subSequence( 0, name.lastIndexOf( ".jap" ) ) )
                            .append( ".ol" ).toString();
            if ( japFile.getEntry( filepath ) == null ) {
                filepath = null;
                filepath = filepath + 'c';
                if ( japFile.getEntry( filepath ) == null ) {
                    filepath = null;
                }
            }
        }

        if ( filepath != null ) {
            filepath = new StringBuilder().append( "jap:file:" ).append( japFile.getName() )
                    .append( "!/" ).append( filepath ).toString();
        }
        return filepath;
    }
}
