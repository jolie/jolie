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
package jolie.jtracer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.tracer.EmbeddingTraceAction;
import jolie.tracer.ErrorTraceAction;
import jolie.tracer.FaultTraceAction;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.TraceAction;
import jolie.tracer.Tracer;
import jolie.tracer.VariableTraceAction;
import joliex.util.JsonUtilsService;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * 
 * @author Vincenzo Mattarella
 */
@AndJarDeps({"commons-lang3.jar", "jolie-js.jar"})
public class JTracer implements Tracer {

    private int actionCounter = 0;
    private PrintWriter JSONWriter;
    private FileWriter writer;
    private final String programFilename;
    private final long fileTimestamp;
    private final String outputName;

    public JTracer(String programFilename, long timestamp) {
        this.programFilename = programFilename;
        this.fileTimestamp = timestamp;
        String fileName = parsedTimestamp();
        //Name of the file which will be created (if not exixts yet) and saved
        outputName = fileName + " - " + programFilename.substring(0, programFilename.length() - 3) + "_trace.json";
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
        } else if (action instanceof ErrorTraceAction) {
            trace((ErrorTraceAction) action);
        } else if (action instanceof FaultTraceAction) {
            trace((FaultTraceAction) action);
        }
    }

    public void trace(FaultTraceAction action) {
        StringBuilder stBuilder = new StringBuilder();
        if (isEmptyFile()) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Action\" : \"");
        switch (action.type()) {
            case FAULT_SCOPE:
                stBuilder.append("SCOPE");
                break;
            case FAULT_INSTALL:
                stBuilder.append("INSTALL");
                break;
            case FAULT_THROW:
                stBuilder.append("THROW");
                break;
            default:
                break;
        }

        String assignmentValue = StringEscapeUtils.escapeJava(action.value());
        stBuilder
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\", \"Instance\" : \"").append(action.sessionId())
                .append("\", \"Description\" : \"").append(assignmentValue).append("\"}");
        write(stBuilder.toString());
        closeWriter();
    }

    public void trace(ErrorTraceAction action) {
        StringBuilder stBuilder = new StringBuilder();
        if (isEmptyFile()) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Action\" : \"");
        switch (action.type()) {
            case ERROR_LOGINFO:
                stBuilder.append("LOGINFO");
                break;
            case ERROR_WARNING:
                stBuilder.append("WARNING");
                break;
            case ERROR_FINE:
                stBuilder.append("FINE");
                break;
            case ERROR_SEVERE:
                stBuilder.append("SEVERE");
                break;
            default:
                break;
        }
        stBuilder
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\", \"Instance\" : \"").append(action.sessionId())
                .append("\" , \"Message\" : \"").append(action.message()).append("\"}");
        write(stBuilder.toString());
        closeWriter();
    }

    public void trace(VariableTraceAction action) {
        StringBuilder stBuilder = new StringBuilder();
        if (isEmptyFile()) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Action\" : \"");
        switch (action.type()) {
            case ASSIGNMENT:
                stBuilder.append("ASSIGNMENT");
                break;
            case PREDECREMENT:
                stBuilder.append("PREDECREMENT");
                break;
            case POSTDECREMENT:
                stBuilder.append("POSTDECREMENT");
                break;
            case PREINCREMENT:
                stBuilder.append("PREINCREMENT");
                break;
            case POSTINCREMENT:
                stBuilder.append("POSTINCREMENT");
                break;
            default:
                break;
        }
        String assignmentValue = StringEscapeUtils.escapeJava(action.origin());
        stBuilder
                .append("\", \"Operator\" : \"").append(action.variableName())
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\", \"ValueToAssign\" : \"").append(assignmentValue)
                .append("\", \"Instance\" : \"").append(action.sessionId())
                .append("\" , \"Description\" : \"").append(action.description()).append("\"}");
        write(stBuilder.toString());
        closeWriter();
    }

    public void trace(EmbeddingTraceAction action) {
        StringBuilder stBuilder = new StringBuilder();
        if (isEmptyFile()) {
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
                .append("\", \"Operator\" : \"").append(action.name())
                .append("\", \"Timestamp\" : \"").append(action.timeValue())
                .append("\", \"Instance\" : \"").append(action.sessionId())
                .append("\" , \"Description\" : \"").append(action.description()).append("\"}");
        write(stBuilder.toString());
        closeWriter();
    }

    public void trace(MessageTraceAction action) {

        StringBuilder stBuilder = new StringBuilder();
        if (isEmptyFile()) {
            stBuilder.append("{\"ServiceName\" : \"");
        } else {
            stBuilder.append(",{\"ServiceName\" : \"");
        }
        stBuilder.append(programFilename).append("\", \"InstructionNumber\" : \"");
        stBuilder.append(Integer.toString(actionCounter)).append("\", \"Action\" : \"");
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
                .append("\", \"Operator\" : \"")
                .append(action.name())
                .append("\", \"Description\" : \"").append(action.description())
                .append("\", \"Instance\" : \"").append(action.sessionId())
                .append("\", \"Timestamp\" : \"").append(action.timeValue());

        if (action.message() != null) {

            stBuilder.append("\", \"MSG_ID\" : \"").append(action.message().id());
            Writer writer = new StringWriter();
            Value messageValue = action.message().value();
            if (action.message().isFault()) {
                messageValue = action.message().fault().value();
                stBuilder.append("\"}");
            } else {
                JsonUtilsService j = new JsonUtilsService();
                try {
                    Value val = j.getJsonString(messageValue);
                    stBuilder.append("\", \"Value\" : ").append(val.strValue()).append("}");
                } catch (FaultException ex) {
                    Logger.getLogger(JTracer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } else {
            stBuilder.append("\"}");
        }
        write(stBuilder.toString());
        closeWriter();
    }

    /*
        Handles both opening and writing procedures 
    */
    private void write(String contentToWrite) {
        try {
            writer = new FileWriter(outputName, true);
            JSONWriter = new PrintWriter(writer);
            JSONWriter.println(contentToWrite);
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
    }

    /*
        Handles closing procedure
    */
    private void closeWriter() {
        JSONWriter.flush();
        JSONWriter.close();
    }

    /*
        Checks if file on which we would like to write is empty or not
    */
    private boolean isEmptyFile() {
        File file = new File(outputName);
        return file.length() == 0;
    }

    /*
        Return a String value of the current date
    */
    private String parsedTimestamp() {
        Date currentDate = new Date(fileTimestamp);
        SimpleDateFormat dateFormatter = new SimpleDateFormat();
        dateFormatter.applyPattern("dd.MM.yy - HH.mm.ss.SSS");
        return dateFormatter.format(currentDate);
    }
}
