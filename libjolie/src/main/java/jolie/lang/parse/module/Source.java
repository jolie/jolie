package jolie.lang.parse.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import jolie.lang.Constants;

public interface Source
{
    URI source();

    Optional<InputStream> stream();
}


class FileSource implements Source
{

    private final File file;

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
    public Optional<InputStream> stream()
    {
        try {
            return Optional.of(new FileInputStream( this.file ));
        } catch (FileNotFoundException e) { return Optional.empty(); }
    }
}


class JapSource implements Source
{

    private final JarFile japFile;
    private final URI source;
    private final String filePath;
    private final String parentPath;

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
    public URI source()
    {
        return this.source;
    }

    @Override
    public Optional<InputStream> stream()
    {
        try {
            ZipEntry z = japFile.getEntry( this.filePath );
            return Optional.of(this.japFile.getInputStream( z ));
        } catch (IOException e) { return Optional.empty(); }
    }
}
