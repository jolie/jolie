package joliex.wsdl;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import jolie.cli.CommandLineException;
import jolie.JolieURLStreamHandlerFactory;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

/**
 *
 * @author Francesco Bullini and Claudio Guidi
 */
public class Jolie2Wsdl {

	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}

	public static void main( String[] args ) {
		try {
			Jolie2WsdlCommandLineParser cmdParser =
				Jolie2WsdlCommandLineParser.create( args, Jolie2Wsdl.class.getClassLoader() );
			args = cmdParser.getInterpreterConfiguration().arguments();

			Program program = ParsingUtils.parseProgram(
				cmdParser.getInterpreterConfiguration().source(),
				cmdParser.getInterpreterConfiguration().charset(),
				cmdParser.getInterpreterConfiguration().includePaths(),
				cmdParser.getInterpreterConfiguration().packagePaths(),
				cmdParser.getInterpreterConfiguration().jolieClassLoader(),
				cmdParser.getInterpreterConfiguration().constants(),
				cmdParser.getInterpreterConfiguration().executionTarget(),
				false );

			// Program program = parser.parse();
			ProgramInspector inspector = ParsingUtils.createInspector( program );

			WSDLDocCreator document = new WSDLDocCreator( inspector, program.context().source() );
			String outfile = cmdParser.getOutputFile();
			String tns = cmdParser.getNamespace();
			String portName = cmdParser.getPortName();
			String address = cmdParser.getAddress();

			if( outfile == null || tns == null || portName == null || address == null ) {
				System.out.println( cmdParser.getHelpString() );
			} else {
				document.ConvertDocument( outfile, tns, portName, address );
			}
		} catch( CommandLineException | ParserException ex ) {
			System.out.println( ex.getMessage() );
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
}
