package joliex.java.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import joliex.java.generate.type.TypeClassBuilder;
import joliex.java.generate.util.ClassBuilderException;

public class JavaClassDirector {

    public static String constructClass( JavaClassBuilder builder ) {
        builder.appendHeader();
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

    public static void writeClass( JavaClassBuilder builder, String directory ) {
        try {
            Writer writer = new BufferedWriter( new FileWriter( directory + File.separator + builder.className() + ".java" ) );
            writer.append( constructClass( builder ) );
            writer.flush();
            writer.close();
        } catch( IOException ex ) {
            Logger.getLogger( JavaClassDirector.class.getName() ).log( Level.SEVERE, null, ex );
        } catch ( ClassBuilderException ex ) {
            ex.printStackTrace();
        }
    }
}
