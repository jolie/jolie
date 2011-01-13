/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package joliedummycreator;

import java.io.IOException;
import java.net.URI;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import joliedummycreator.impl.JolieDummyDocumentCreator;

/**
 *
 * @author balint
 */
public class JolieDummyCreator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException  {
       try{
//		CommandLineParser cmdParser =  new CommandLineParser( args, JolieDummyCreator.class.getClassLoader() );
//			args = cmdParser.arguments();
//
//
//			Program program = ParsingUtils.parseProgram(
//				cmdParser.programStream(),
//				URI.create( "file:" + cmdParser.programFilepath() ),
//				cmdParser.includePaths(), JolieDummyCreator.class.getClassLoader(), cmdParser.definedConstants()
//			);
		   JolieDummyCommandLineParser cmdParser= JolieDummyCommandLineParser.create( args, JolieDummyCommandLineParser.class.getClassLoader() );
           Program program = ParsingUtils.parseProgram(
			cmdParser.programStream(),
				URI.create( "file:" + cmdParser.programFilepath() ),
				cmdParser.includePaths(), JolieDummyCreator.class.getClassLoader(), cmdParser.definedConstants());
			ProgramInspector inspector=ParsingUtils.createInspector( program );
            JolieDummyDocumentCreator document= new JolieDummyDocumentCreator( inspector,cmdParser.programFilepath());
            document.createDocument();

		} catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			e.printStackTrace();
		}
    }

}
