/**
 * *******************************************************************************
 * Copyright (C) 2006-2014 by Fabrizio Montesi <famontesi@gmail.com> * * This
 * program is free software; you can redistribute it and/or modify * it under
 * the terms of the GNU Library General Public License as * published by the
 * Free Software Foundation; either version 2 of the * License, or (at your
 * option) any later version. * * This program is distributed in the hope that
 * it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. * * You should have received a
 * copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 * *******************************************************************************
 */
package jolie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import jolie.lang.parse.ParserException;
import jolie.tracer.ErrorTraceAction;
import jolie.tracer.Tracer;

/**
 * Starter class of the Interpreter.
 *
 * @author Fabrizio Montesi
 */
public class Jolie {

    static {
        JolieURLStreamHandlerFactory.registerInVM();
    }

    private Jolie() {
    }

    private static final long TERMINATION_TIMEOUT = 500; // 0.5 seconds
/*
    private static PrintWriter JSONWriter;
    private static FileWriter writer;
    private static String stringedTimestamp;
    private static StringBuilder stBuilder;
*/
    /**
     * Entry point of program execution.
     *
     * @param args the command line arguments TODO Standardize the exit codes.
     */
    public static void main(String[] args) {
        long timestamp = System.currentTimeMillis();
        int exitCode = 0;
        try {
            /*
            stringedTimestamp = parsedTimestamp(timestamp);
            writer = new FileWriter(stringedTimestamp + ".json", true);
            JSONWriter = new PrintWriter(writer);
            */
            final Interpreter interpreter = new Interpreter(timestamp, args, Jolie.class.getClassLoader(), null);
            Thread.currentThread().setContextClassLoader(interpreter.getClassLoader());
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    interpreter.exit(TERMINATION_TIMEOUT);
                }
            });
            interpreter.run();
        } catch (CommandLineException cle) {
            //write(cle.getMessage());
            System.out.println(cle.getMessage());
        } catch (FileNotFoundException fe) {
            //write(fe.getMessage());
            fe.printStackTrace();
            exitCode = 1;
        } catch (IOException ioe) {
            //write(ioe.getMessage());
            ioe.printStackTrace();
            exitCode = 2;
        } catch (InterpreterException ie) {
            if (ie.getCause() instanceof ParserException) {
                //write(ie.getCause().getMessage());
                ie.getCause().printStackTrace();
            } else {
                //write(ie.getMessage());
                ie.printStackTrace();
            }
            exitCode = 3;
        } catch (Exception e) {
            //write(e.getMessage());
            e.printStackTrace();
            exitCode = 4;
        }
        System.exit(exitCode);
    }
/*
    private static String parsedTimestamp(long timestamp) {
        
        Date currentDate = new Date(timestamp);
        SimpleDateFormat dateFormatter = new SimpleDateFormat();
        dateFormatter.applyPattern("dd.MM.yy - HH.mm.ss.SSS");
        return dateFormatter.format(currentDate);
        
    }
    
    private static void buildString(String message) {
        
        stBuilder = new StringBuilder();
        if (emptyFile()) {
            stBuilder.append("{\"Error\" : \"");
        } else {
            stBuilder.append(",{\"Error\" : \"");
        }
        stBuilder.append(message);
        stBuilder.append("\"}");
        
    }


    private static boolean emptyFile() {

        File file = new File(stringedTimestamp + ".json");
        boolean empty = file.length() == 0;
        return empty;
    }

    private static void write(String message) {
        
        buildString(message);
        JSONWriter.println(stBuilder);
        JSONWriter.flush();
        JSONWriter.close();
        
    }
    */
}
