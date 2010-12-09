package joliex.wsdl;



import joliex.wsdl.alternative.baseversion.Jolie2wsdlVisitor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class Jolie2wsdlTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public Jolie2wsdlTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( Jolie2wsdlTest.class );
    }

    /**
     * TODO Aggiungere implementazione della toString()  alle principali classi dell'AST, e poi fare metodo di debug automatico durante la visita
     * NOTA-BENE: questo non è un test ancora; in futuro si caricheranno i risultati attesi e si confronteranno con quelli risultato delle  elaborati;
     */
    public void testApp()
    {
        InputStream olStream = null;
        try {
            String inputFileName = "./provaInputPorts.ol"; //args[0]
            olStream = new FileInputStream(inputFileName);
			Program program = ParsingUtils.parseProgram(
				olStream,
				URI.create( "file:" + inputFileName ),
				new String[] { "." }, Thread.currentThread().getContextClassLoader(), new HashMap< String, Scanner.Token >()
			);
            System.out.println(" ============= STAMPA del PROGRAM verificato =============");
            System.out.println(" parsedProgram=" + program);
            List<OLSyntaxNode> nodes = program.children();
            for (OLSyntaxNode node : nodes) {
                System.out.println(node);
            }
            System.out.println(" =========== CHECK SEM ===============");
            program = (new OLParseTreeOptimizer(program)).optimize();
            if (!(new SemanticVerifier(program)).validate()) {
                throw new IOException("Exiting");
            }
            System.out.println(" =========== VISITA ===============");
            Jolie2wsdlVisitor docVisitor = new Jolie2wsdlVisitor(program);
            docVisitor.visit(program);
            System.out.println(" ============= STAMPA del PROGRAM visitato =============");
            nodes = program.children();
            for (OLSyntaxNode node : nodes) {
                System.out.println(node);
            }
        } catch (ParserException ex) {
            Logger.getLogger(Jolie2wsdlTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Jolie2wsdlTest.class.getName()).log(Level.SEVERE, null, ex);
        }
//        catch (FileNotFoundException ex) {
//            Logger.getLogger(Jolie2wsdlTest.class.getName()).log(Level.SEVERE, null, ex);
//        } 
        finally {
            try {
                olStream.close();
            } catch (IOException ex) {
                Logger.getLogger(Jolie2wsdlTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
}
