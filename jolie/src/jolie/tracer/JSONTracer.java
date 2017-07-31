/*
 * Copyright (C) 2017 Vincenzo Mattarella.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jolie.tracer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;
import jolie.runtime.Value;
import jolie.runtime.ValueJSONPrinter;
import jolie.runtime.ValuePrettyPrinter;

/**
 *
 * @author Vincenzo Mattarella
 */
public class JSONTracer implements Tracer {

    private int actionCounter = 0;
    private PrintWriter JSONWriter;
    private FileWriter writer;
    private final String programFilename;
    private final long fileTimestamp;

    public JSONTracer(String programFilename, long timestamp) {
        this.programFilename = programFilename;
        this.fileTimestamp = timestamp;
    }

    @Override
    public synchronized void trace(Supplier<? extends TraceAction> supplier) {

        final TraceAction action = supplier.get();
        actionCounter++;
        if (action instanceof MessageTraceAction) {
            trace((MessageTraceAction) action);
        } else if (action instanceof EmbeddingTraceAction) {
            trace((EmbeddingTraceAction) action);
        } else if (action instanceof VariableTraceAction) {
            trace((VariableTraceAction) action);
        }else if(action instanceof ErrorTraceAction){
            trace((ErrorTraceAction) action);
        }else if(action instanceof FaultTraceAction){
            trace((FaultTraceAction) action);
        }
    }
    
    public void trace(FaultTraceAction action){
        String fileName = parsedTimestamp();
        try {
            writer = new FileWriter(fileName + ".json", true);
            JSONWriter = new PrintWriter(writer);

        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        File file = new File(fileName + ".json");
        boolean empty = file.length() == 0;
        StringBuilder stBuilder = new StringBuilder();
        if (empty) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Type\" : \"");
        switch (action.type()) {
            case FAULT_SCOPE:
                stBuilder.append("FAULT_SCOPE");
                break;
            case FAULT_INSTALL:
                stBuilder.append("FAULT_INSTALL");
                break;
            case FAULT_THROW:
                stBuilder.append("FAULT_THROW");
                break;
            default:
                break;
        }
        stBuilder
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\", \"Description\" : \"").append(action.value())
                .append("\" , \"Action\" : \"").append(action.message()).append("\"}");
        System.out.println(stBuilder.toString());
        JSONWriter.println(stBuilder.toString());
        JSONWriter.flush();
        JSONWriter.close();
        
    }
    
    public void trace(ErrorTraceAction action){
        String fileName = parsedTimestamp();
        try {
            writer = new FileWriter(fileName + ".json", true);
            JSONWriter = new PrintWriter(writer);

        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        File file = new File(fileName + ".json");
        boolean empty = file.length() == 0;
        StringBuilder stBuilder = new StringBuilder();
        if (empty) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Type\" : \"");
        switch (action.type()) {
            case ERROR_LOGINFO:
                stBuilder.append("ERROR_LOGINFO");
                break;
            case ERROR_WARNING:
                stBuilder.append("ERROR_WARNING");
                break;
            case ERROR_FINE:
                stBuilder.append("ERROR_FINE");
                break;
            case ERROR_SEVERE:
                stBuilder.append("ERROR_SEVERE");
                break;
            default:
                break;
        }
        stBuilder
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\" , \"Message\" : \"").append(action.message()).append("\"}");
        System.out.println(stBuilder.toString());
        JSONWriter.println(stBuilder.toString());
        JSONWriter.flush();
        JSONWriter.close();
    }
    
    public void trace(VariableTraceAction action) {
        String fileName = parsedTimestamp();
        try {
            writer = new FileWriter(fileName + ".json", true);
            JSONWriter = new PrintWriter(writer);

        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        File file = new File(fileName + ".json");
        boolean empty = file.length() == 0;
        StringBuilder stBuilder = new StringBuilder();
        if (empty) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Type\" : \"");
        switch (action.type()) {
            case ASSIGNMENT:
                stBuilder.append("VARIABLE");
                break;
            default:
                break;
        }
        stBuilder
                .append("\", \"Name\" : \"").append(action.name())
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\", \"ValueToAssign\" : \"").append(action.origin())
                .append("\", \"VarName\" : \"").append(action.variableName())
                .append("\" , \"Description\" : \"").append(action.description()).append("\"}");
        System.out.println(stBuilder.toString());
        JSONWriter.println(stBuilder.toString());
        JSONWriter.flush();
        JSONWriter.close();
        //System.out.println("Vins: "  + action.description()+ " - " +  action.value().valueObject().toString());
    }

    public void trace(EmbeddingTraceAction action) {
        String fileName = parsedTimestamp();
        try {
            writer = new FileWriter(fileName + ".json", true);
            JSONWriter = new PrintWriter(writer);

        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        File file = new File(fileName + ".json");
        boolean empty = file.length() == 0;
        StringBuilder stBuilder = new StringBuilder();
        if (empty) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Action\" : \"");
        switch (action.type()) {
            case SERVICE_LOAD:
                stBuilder.append("^ LOAD");
                break;
            default:
                break;
        }
        stBuilder
                .append("\", \"Name\" : \"").append(action.name())
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\" , \"Description\" : \"").append(action.description()).append("\"}");
        System.out.println(stBuilder.toString());
        JSONWriter.println(stBuilder.toString());
        JSONWriter.flush();
        JSONWriter.close();
    }

    public void trace(MessageTraceAction action) {

        String fileName = parsedTimestamp();
        try {

            writer = new FileWriter(fileName + ".json", true);
            JSONWriter = new PrintWriter(writer);

        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }

        StringBuilder stBuilder = new StringBuilder();
        File file = new File(fileName + ".json");
        boolean empty = file.length() == 0;
        if (empty) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Type\" : \"");
        switch (action.type()) {
            case SOLICIT_RESPONSE:
                stBuilder.append("SOLICIT RESPONSE");
                break;
            case NOTIFICATION:
                stBuilder.append("NOTIFICATION");
                break;
            case ONE_WAY:
                stBuilder.append("ONE WAY");
                break;
            case REQUEST_RESPONSE:
                stBuilder.append("REQUEST RESPONSE");
                break;
            case COURIER_NOTIFICATION:
                stBuilder.append("COURIER NOTIFICATION");
                break;
            case COURIER_SOLICIT_RESPONSE:
                stBuilder.append("COURIER SOLICIT RESPONSE");
                break;
            case AGGREGATION:
                stBuilder.append("AGGREGATION");
                break;
            default:
                break;
        }
        stBuilder
                .append("\", \"Name\" : \"")
                .append(action.name())
                .append("\", \"Description\" : \"").append(action.description())
                .append("\", \"Timestamp\" : \"").append(action.timeValue());
        if (action.message() != null) {
            stBuilder.append("\", \"MSG_ID\" : \"").append(action.message().id()).append("\",\"Value\" : [");
            Writer writer = new StringWriter();
            Value messageValue = action.message().value();
            if (action.message().isFault()) {
                messageValue = action.message().fault().value();
            }
            ValueJSONPrinter printer = new ValueJSONPrinter(messageValue,writer);
           
            printer.setByteTruncation(50);
            try {
                printer.run();
            } catch (IOException e) {
            } // Should never happen

            stBuilder.append(writer.toString()).append("\"}]}");
        } else {
            stBuilder.append("\"}");
        }
        System.out.println(stBuilder.toString());
        JSONWriter.println(stBuilder.toString());
        JSONWriter.flush();
        JSONWriter.close();
    }

    private String parsedTimestamp() {
        Date currentDate = new Date(fileTimestamp);
        SimpleDateFormat dateFormatter = new SimpleDateFormat();
        dateFormatter.applyPattern("dd.MM.yy - HH.mm.ss.SSS");
        return dateFormatter.format(currentDate);
    }
}
