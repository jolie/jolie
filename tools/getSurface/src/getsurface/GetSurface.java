/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package getsurface;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

/**
 *
 * @author claudio
 */
public class GetSurface {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
           
            CommandLineParser cmdParser = new CommandLineParser(args, GetSurface.class.getClassLoader());
            Program program = ParsingUtils.parseProgram(
                    cmdParser.programStream(),
                    URI.create("file:" + cmdParser.programFilepath()),
                    cmdParser.includePaths(), GetSurface.class.getClassLoader(), cmdParser.definedConstants());
            ProgramInspector inspector = ParsingUtils.createInspector(program);

            SurfaceCreator document = new SurfaceCreator(inspector, program.context().source());
             
            document.ConvertDocument( args[3] );

        } catch (CommandLineException ex) {
            Logger.getLogger(GetSurface.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


