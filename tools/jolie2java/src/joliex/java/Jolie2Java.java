/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.java;

import java.io.IOException;
import jolie.CommandLineException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import joliex.java.impl.JavaDocumentCreator;
import joliex.java.impl.JavaGWTDocumentCreator;
import joliex.java.impl.ProgramVisitor;

/**
 *
 * @author balint
 */
public class Jolie2Java {

    public static void main(String[] args) {
        try {

            Jolie2JavaCommandLineParser cmdParser = Jolie2JavaCommandLineParser.create(args, Jolie2Java.class.getClassLoader());

            Program program = ParsingUtils.parseProgram(
                    cmdParser.programStream(),
                    cmdParser.programFilepath().toURI(), cmdParser.charset(),
                    cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants(), false );

            //Program program = parser.parse();
            ProgramInspector inspector = ParsingUtils.createInspector(program);
            ProgramVisitor visitor = new ProgramVisitor(program);
            visitor.run();

            String format = cmdParser.getFormat();
			String packageName = cmdParser.getPackageName();
			if ( format == null ) { format = "java"; }
            if (format.equals("java") && packageName != null ) {
                JavaDocumentCreator documentJava = new JavaDocumentCreator(inspector, cmdParser.getPackageName(),
                        cmdParser.getTargetPort(), cmdParser.isAddSource(), cmdParser.getOutputDirectory(),
                        cmdParser.isBuildXmlenabled(), cmdParser.getJavaservice());
                documentJava.ConvertDocument();
            } else if (format.equals("gwt") && packageName != null) {
				System.out.println("WARNING: gwt conversion is deprecated, use it at your own risk");
                JavaGWTDocumentCreator documentJava = new JavaGWTDocumentCreator(inspector, cmdParser.getPackageName(), cmdParser.getTargetPort());
                documentJava.ConvertDocument();

            } else {
                System.out.print( cmdParser.getHelpString());
            }

			System.out.println("Generation done!");
        } catch (CommandLineException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        } catch (SemanticException e) {
            e.printStackTrace();
        } catch( FaultException e ) {
            e.printStackTrace();
        }
    }
}
