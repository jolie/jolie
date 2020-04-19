package jolie.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.Scanner;

/**
 * A utility class for instance creation
 */
public class InstanceCreator
{

    String[] includePaths;

    public InstanceCreator( String[] includePaths )
    {
        if ( this.includePaths == null ) {
            this.includePaths = new String[] {};
        }
        this.includePaths = includePaths;
    }

    public OLParser createOLParser( URI Source, InputStream codeSteam )
            throws IOException, URISyntaxException
    {
        return new OLParser( new Scanner( codeSteam, Source, StandardCharsets.UTF_8.name(), false ),
                this.includePaths, InstanceCreator.class.getClassLoader() );
    }


    public OLParser createOLParser( InputStream codeSteam ) throws IOException, URISyntaxException
    {
        return new OLParser(
                new Scanner( codeSteam,
                        InstanceCreator.class.getClassLoader().getResource( "." ).toURI(),
                        StandardCharsets.UTF_8.name(), false ),
                this.includePaths, InstanceCreator.class.getClassLoader() );
    }

    public OLParser createOLParser( Scanner source ) throws IOException, URISyntaxException
    {
        return new OLParser( source, this.includePaths, InstanceCreator.class.getClassLoader() );
    }

}
