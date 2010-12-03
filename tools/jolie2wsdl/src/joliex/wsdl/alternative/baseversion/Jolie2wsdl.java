/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/
package joliex.wsdl.alternative.baseversion;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.Program;

public class Jolie2wsdl {

    public static void execute(String[] args) {
        if (args.length < 1) {
            return;
        }
        try {
            InputStream olStream = new FileInputStream(args[0]);
            OLParser olParser = new OLParser(
                    new Scanner(olStream, args[0]),
                    new String[]{"."},
                    Thread.currentThread().getContextClassLoader());
            Program program = olParser.parse();
			
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
            Jolie2wsdlVisitor jolie2wsdlVisitor = new Jolie2wsdlVisitor(program);
            jolie2wsdlVisitor.visit(program);
            System.out.println(" ============= STAMPA del PROGRAM visitato =============");
            nodes = program.children();
            for (OLSyntaxNode node : nodes) {
                System.out.println(node);
            }
            Definition wd = jolie2wsdlVisitor.getWsdlDef();
            WSDLFactory f = WSDLFactory.newInstance();
            // WSDLReader r = f.newWSDLReader();
            //r.readWSDL("");
            WSDLWriter ww = f.newWSDLWriter();

            FileWriter fw = new FileWriter(args[1]);
            ww.writeWSDL(wd, fw);
        } catch (WSDLException ex) {
            Logger.getLogger(Jolie2wsdl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            e.printStackTrace();

        } catch (ParserException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String fileNameIn = "./provaInputPorts.ol";
        String fileNameOut = "./provaInputPorts.wsdl";
        Desktop desktop;
        String[] myArgs = {fileNameIn, fileNameOut};
        execute(myArgs);
//        if (Desktop.isDesktopSupported()) {
//            try {
//                desktop = Desktop.getDesktop();
//                // Now enable buttons for actions that are supported.
//                File f = new File(fileNameOut);
//
//                desktop.open(f);
//            } catch (IOException ex) {
//                Logger.getLogger(Jolie2wsdl.class.getName()).log(Level.SEVERE, null, ex);
//            }
//    }
    }
}
