package jolie.lang.parse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * A utility class for instance creation
 */
class InstanceCreator
{

    String[] includePaths;

    public InstanceCreator( String[] includePaths )
    {
        if ( this.includePaths == null ) {
            this.includePaths = new String[] {};
        }
        this.includePaths = includePaths;
    }

    OLParser createOLParser( InputStream codeSteam ) throws IOException, URISyntaxException
    {
        return new OLParser(
                new Scanner( codeSteam,
                        InstanceCreator.class.getClassLoader().getResource( "." ).toURI(),
                        StandardCharsets.UTF_8.name(), false ),
                this.includePaths, InstanceCreator.class.getClassLoader() );
    }

    OLParser createOLParser( Scanner source ) throws IOException, URISyntaxException
    {
        return new OLParser( source, this.includePaths, InstanceCreator.class.getClassLoader() );
    }

}
