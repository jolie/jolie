package joliex.java.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import joliex.java.generate.type.TypeClassBuilder;
import joliex.java.generate.util.ClassBuilderException;

public class JavaClassDirector {

    public static String constructClass( JavaClassBuilder builder ) {
        builder.appendPackage();
        builder.appendDefinition();
        return builder.getResult();
    }

    public static String constructInnerClass( JavaClassBuilder builder ) {
        switch ( builder ) {
            case TypeClassBuilder b -> b.appendDefinition( true );
            default -> builder.appendDefinition();
        }
        return builder.getResult();
    }

    public static void writeClass( Path dir, JavaClassBuilder builder, boolean override ) {
        try {
            final Path file = dir.resolve( builder.className() + ".java" );
            if ( override || Files.notExists( file ) ) {
                Files.createDirectories( dir );
                Files.writeString( file, constructClass( builder ) );
            }
        } catch( IOException ex ) {
            Logger.getLogger( JavaClassDirector.class.getName() ).log( Level.SEVERE, null, ex );
        } catch ( ClassBuilderException ex ) {
            ex.printStackTrace();
        }
    }
}
