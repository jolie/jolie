package joliex.wsdl;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


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
 * @author Francesco Bullini and Claudio Guidi
 */
public class Jolie2Wsdl
{
	public static void main( String[] args )
	{
		try {
			CommandLineParser cmdParser = new CommandLineParser( args, Jolie2Wsdl.class.getClassLoader() );
			args = cmdParser.arguments();
			Program program = ParsingUtils.parseProgram(
				cmdParser.programStream(),
				URI.create( "file:" + cmdParser.programFilepath() ),
				cmdParser.includePaths(), Jolie2Wsdl.class.getClassLoader(), cmdParser.definedConstants() );
			ProgramInspector inspector=ParsingUtils.createInspector( program );
	
			WSDLDocCreator document= new WSDLDocCreator( inspector, program.context().source());
                        try {

                            String outfile = "";
                            String tns ="";
                            if ( args.length == 0 ) {
                                throw( new Exception("Usage: jolie2wsdl targetNameSpace [output filename]"));
                            } else {
                                if ( args.length >= 1 ) {
                                    tns = args[0];
                                    if ( args.length > 1 ) {
                                        outfile=args[1];
                                    } else {
                                        outfile = "MyWsdl.wsdl";
                                    }
                                }
                            }
                            document.ConvertDocument( outfile, tns  );
                        } catch ( Jolie2WsdlException e ) {
                            System.out.println( e.getMessage() );
                        }
		} 
		catch( CommandLineException ex ) {
			Logger.getLogger( Jolie2Wsdl.class.getName() ).log( Level.SEVERE, null, ex );
		}		catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			System.out.println( e.getMessage() );
		} catch( Exception e )  {
                    e.printStackTrace();
                }
	}
}
