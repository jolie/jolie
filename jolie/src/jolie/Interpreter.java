/**
 * *************************************************************************
 * Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com> * * This
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
 * *************************************************************************
 */
package jolie;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import jolie.lang.Constants;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.TypeChecker;
import jolie.lang.parse.ast.Program;
import jolie.monitoring.MonitoringEvent;
import jolie.monitoring.events.MonitorAttachedEvent;
import jolie.monitoring.events.OperationStartedEvent;
import jolie.monitoring.events.SessionEndedEvent;
import jolie.monitoring.events.SessionStartedEvent;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.SessionMessage;
import jolie.net.ports.OutputPort;
import jolie.process.DefinitionProcess;
import jolie.process.InputOperationProcess;
import jolie.process.SequentialProcess;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.OneWayOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.TimeoutHandler;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.correlation.CorrelationEngine;
import jolie.runtime.correlation.CorrelationError;
import jolie.runtime.correlation.CorrelationSet;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderFactory;
import jolie.tracer.DummyTracer;
import jolie.tracer.ErrorTraceAction;
import jolie.tracer.PrintingTracer;
import jolie.tracer.Tracer;

/**
 * The Jolie interpreter engine. Multiple Interpreter instances can be run in
 * the same JavaVM; this is used, e.g., for service embedding.
 *
 * @author Fabrizio Montesi
 */
public class Interpreter {

    private class InitSessionThread extends SessionThread {

        public InitSessionThread(Interpreter interpreter, jolie.process.Process process) {
            super(interpreter, process);
            addSessionListener(new SessionListener() {
                public void onSessionExecuted(SessionThread session) {
                    onSuccessfulInitExecution();
                }

                public void onSessionError(SessionThread session, FaultException fault) {
                    exit();
                }
            });
        }

        private void onSuccessfulInitExecution() {
            if (executionMode == Constants.ExecutionMode.SINGLE) {
                synchronized (correlationEngine) {
                    try {
                        mainSession = new SessionThread(getDefinition("main"), initExecutionThread);
                        correlationEngine.onSingleExecutionSessionStart(mainSession);
                        mainSession.addSessionListener(correlationEngine);
                        correlationEngine.onSessionExecuted(this);
                    } catch (InvalidIdException e) {
                        assert false;
                    }
                }
            } else {
                correlationEngine.onSessionExecuted(this);
            }

            /*
             * We need to relay the messages we did not consume during the init procedure.
             * We do this asynchronously, because calling correlationEngine.onMessageReceive
             * will trigger a join() on this thread, leading to a deadlock if we
             * were to call that directly from here.
             */
            execute(new Runnable() {
                private void pushMessages(Deque< SessionMessage> queue) {
                    for (SessionMessage message : queue) {
                        try {
                            correlationEngine.onMessageReceive(message.message(), message.channel());
                        } catch (CorrelationError e) {
                            logWarning(e);
                            try {
                                message.channel().send(CommMessage.createFaultResponse(message.message(), new FaultException("CorrelationError", "The message you sent can not be correlated with any session and can not be used to start a new session.")));
                            } catch (IOException ioe) {
                                logSevere(ioe);
                            }
                        }
                    }
                }

                public void run() {
                    for (Deque< SessionMessage> queue : messageQueues.values()) {
                        pushMessages(queue);
                    }
                    pushMessages(uncorrelatedMessageQueue);
                }
            });
        }

        @Override
        public boolean isInitialisingThread() {
            return true;
        }
    }

    private static class JolieExecutionThreadFactory implements ThreadFactory {

        private final Interpreter interpreter;

        public JolieExecutionThreadFactory(Interpreter interpreter) {
            this.interpreter = interpreter;
        }

        public Thread newThread(Runnable r) {
            JolieExecutorThread t = new JolieExecutorThread(r, interpreter);
            if (r instanceof ExecutionThread) {
                t.setExecutionThread((ExecutionThread) r);
            }
            return t;
        }
    }

    private static class NativeJolieThreadFactory implements ThreadFactory {

        private final Interpreter interpreter;

        public NativeJolieThreadFactory(Interpreter interpreter) {
            this.interpreter = interpreter;
        }

        public Thread newThread(Runnable r) {
            return new NativeJolieThread(interpreter, r);
        }
    }

    public static class SessionStarter {

        private final InputOperationProcess guard;
        private final jolie.process.Process body;
        private CorrelationSet correlationInitializer = null;

        public SessionStarter(InputOperationProcess guard, jolie.process.Process body) {
            this.guard = guard;
            this.body = body;
        }

        public InputOperationProcess guard() {
            return guard;
        }

        public jolie.process.Process body() {
            return body;
        }

        public void setCorrelationInitializer(CorrelationSet cset) {
            correlationInitializer = cset;
        }

        public CorrelationSet correlationInitializer() {
            return correlationInitializer;
        }
    }

    private CommCore commCore;
    private CommandLineParser cmdParser;
    private Program internalServiceProgram = null;
    private Interpreter parentInterpreter = null;

    private Map< String, SessionStarter> sessionStarters = new HashMap<>();
    private boolean exiting = false;
    private final Lock exitingLock;
    private final Condition exitingCondition;
    private final CorrelationEngine correlationEngine;
    private final List< CorrelationSet> correlationSets = new ArrayList<>();
    private final Map< String, CorrelationSet> operationCorrelationSetMap = new HashMap<>();
    private Constants.ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
    private final Value globalValue = Value.createRootValue();
    private final String[] arguments;
    private final Collection< EmbeddedServiceLoader> embeddedServiceLoaders = new ArrayList<>();
    private static final Logger logger = Logger.getLogger("Jolie");

    private final Map< String, DefinitionProcess> definitions = new HashMap<>();
    private final Map< String, OutputPort> outputPorts = new HashMap<>();
    private final Map< String, InputOperation> inputOperations = new HashMap<>();

    private final HashMap< String, Object> locksMap = new HashMap<>();

    private final ClassLoader parentClassLoader;
    private final String[] includePaths;
    private final String[] optionArgs;
    private final String logPrefix;
    private final Tracer tracer;
    private boolean check = false;
    private final Timer timer;
    // private long inputMessageTimeout = 24 * 60 * 60 * 1000; // 1 day
    private final long persistentConnectionTimeout = 60 * 60 * 1000; // 1 hour
    private final long awaitTerminationTimeout = 60 * 1000; // 1 minute
    // private long persistentConnectionTimeout = 2 * 60 * 1000; // 4 minutes
    // private long persistentConnectionTimeout = 1;

    // 11 is the default initial capacity for PriorityQueue
    private final Queue< WeakReference< TimeoutHandler>> timeoutHandlerQueue
            = new PriorityQueue<>(11, new TimeoutHandler.Comparator());

    private final ExecutorService timeoutHandlerExecutor
            = Executors.newSingleThreadExecutor(new NativeJolieThreadFactory(this));

    private final String programFilename;
    private final File programDirectory;
    private OutputPort monitor = null;

    private long timestamp;

    public void setMonitor(OutputPort monitor) {
        this.monitor = monitor;
        fireMonitorEvent(new MonitorAttachedEvent());
    }

    public boolean isMonitoring() {
        return monitor != null;
    }

    /*public long inputMessageTimeout()
     {
     return inputMessageTimeout;
     }*/
    public String logPrefix() {
        return logPrefix;
    }

    public Tracer tracer() {
        return tracer;
    }

    public void fireMonitorEvent(MonitoringEvent event) {
        if (monitor != null) {
            CommMessage m = CommMessage.createRequest("pushEvent", "/", MonitoringEvent.toValue(event));
            CommChannel channel = null;
            try {
                channel = monitor.getCommChannel();
                channel.send(m);
                CommMessage response;
                do {
                    response = channel.recvResponseFor(m);
                } while (response == null);
            } catch (URISyntaxException e) {
                logWarning(e);
            } catch (IOException e) {
                logWarning(e);
            } finally {
                if (channel != null) {
                    try {
                        channel.release();
                    } catch (IOException e) {
                        logWarning(e);
                    }
                }
            }
        }
    }

    public long persistentConnectionTimeout() {
        return persistentConnectionTimeout;
    }

    public CorrelationEngine correlationEngine() {
        return correlationEngine;
    }

    public void schedule(TimerTask task, long delay) {
        if (exiting == false) {
            timer.schedule(task, delay);
        }
    }

    public void addTimeoutHandler(TimeoutHandler handler) {
        synchronized (timeoutHandlerQueue) {
            timeoutHandlerQueue.add(new WeakReference< TimeoutHandler>(handler));
            if (timeoutHandlerQueue.size() == 1) {
                schedule(new TimerTask() {
                    public void run() {
                        synchronized (timeoutHandlerQueue) {
                            checkForExpiredTimeoutHandlers();
                        }
                    }
                }, handler.time() - System.currentTimeMillis() + 1);
            } else {
                checkForExpiredTimeoutHandlers();
            }
        }
    }

    /*public void removeTimeoutHandler( TimeoutHandler handler )
     {
     synchronized( timeoutHandlerQueue ) {
     timeoutHandlerQueue.remove( handler );
     checkForExpiredTimeoutHandlers();
     }
     }*/
    private void checkForExpiredTimeoutHandlers() {
        long currentTime = System.currentTimeMillis();
        WeakReference< TimeoutHandler> whandler = timeoutHandlerQueue.peek();
        boolean keepRun = true;
        TimeoutHandler handler;
        while (whandler != null && keepRun) {
            handler = whandler.get();
            if (handler == null) {
                timeoutHandlerQueue.remove();
                whandler = timeoutHandlerQueue.peek();
            } else if (handler.time() < currentTime || exiting) {
                // final TimeoutHandler h = handler;
                timeoutHandlerExecutor.execute(handler);
                timeoutHandlerQueue.poll();
                whandler = timeoutHandlerQueue.peek();
            } else {
                keepRun = false;
            }
        }
    }

    /**
     * Returns the option arguments passed to this Interpreter.
     *
     * @return the option arguments passed to this Interpreter
     */
    public String[] optionArgs() {
        return optionArgs;
    }

    /**
     * Returns the include paths this Interpreter is considering.
     *
     * @return the include paths this Interpreter is considering
     */
    public String[] includePaths() {
        return includePaths;
    }

    /**
     * Registers a session starter on this <code>Interpreter</code>.
     *
     * @param guard the input guard for this session starter
     * @param body the body of this session starter
     */
    public void registerSessionStarter(InputOperationProcess guard, jolie.process.Process body) {
        sessionStarters.put(guard.inputOperation().id(), new SessionStarter(guard, body));
    }

    private JolieClassLoader classLoader;

    /**
     * Returns the output ports of this Interpreter.
     *
     * @return the output ports of this Interpreter
     */
    public Collection< OutputPort> outputPorts() {
        return outputPorts.values();
    }

    /**
     * Returns the InputOperation identified by key.
     *
     * @param key the name of the InputOperation to return
     * @return the InputOperation identified by key
     * @throws jolie.runtime.InvalidIdException if this Interpreter does not own
     * the requested InputOperation
     * @see InputOperation
     */
    public InputOperation getInputOperation(String key)
            throws InvalidIdException {
        InputOperation ret = inputOperations.get(key);
        if (ret == null) {
            throw new InvalidIdException(key);
        }
        return ret;
    }

    /**
     * As {@link #getInputOperation(String) getInputOperation}, with the
     * additional constraint that key must identify a OneWayOperation.
     *
     * @param key the name of the OneWayOperation to return
     * @return the OneWayOperation identified by key
     * @throws jolie.runtime.InvalidIdException if this Interpreter does not own
     * the requested OneWayOperation
     * @see OneWayOperation
     */
    public OneWayOperation getOneWayOperation(String key)
            throws InvalidIdException {
        InputOperation ret;
        if ((ret = inputOperations.get(key)) == null || !(ret instanceof OneWayOperation)) {
            throw new InvalidIdException(key);
        }
        return (OneWayOperation) ret;
    }

    /**
     * As {@link #getInputOperation(String) getInputOperation}, with the
     * additional constraint that key must identify a RequestResponseOperation.
     *
     * @param key the name of the RequestResponseOperation to return
     * @return the RequestResponseOperation identified by key
     * @throws jolie.runtime.InvalidIdException if this Interpreter does not own
     * the requested RequestResponseOperation
     * @see RequestResponseOperation
     */
    public RequestResponseOperation getRequestResponseOperation(String key)
            throws InvalidIdException {
        InputOperation ret;
        if ((ret = inputOperations.get(key)) == null || !(ret instanceof RequestResponseOperation)) {
            throw new InvalidIdException(key);
        }
        return (RequestResponseOperation) ret;
    }

    /**
     * Returns the OutputPort identified by key.
     *
     * @param key the name of the OutputPort to return
     * @return the OutputPort identified by key
     * @throws jolie.runtime.InvalidIdException if this Interpreter does not own
     * the requested OutputPort
     */
    public synchronized OutputPort getOutputPort(String key)
            throws InvalidIdException {
        OutputPort ret;
        if ((ret = outputPorts.get(key)) == null) {
            throw new InvalidIdException(key);
        }
        return (OutputPort) ret;
    }

    /**
     * Removes a registered OutputPort.
     *
     * @param key the name of the OutputPort to remove
     */
    public synchronized void removeOutputPort(String key) {
        outputPorts.remove(key);
    }

    /**
     * Returns the Definition identified by key.
     *
     * @param key the name of the Definition to return
     * @return the Definition identified by key
     * @throws jolie.runtime.InvalidIdException if this Interpreter does not own
     * the requested Definition
     */
    public DefinitionProcess getDefinition(String key)
            throws InvalidIdException {
        DefinitionProcess ret;
        if ((ret = definitions.get(key)) == null) {
            throw new InvalidIdException(key);
        }
        return ret;
    }

    /**
     * Registers an <code>OutputPort</code> on this interpreter.
     *
     * @param key the name of the <code>OutputPort</code> to register
     * @param value the <code>OutputPort</code> to register
     */
    public void register(String key, OutputPort value) {
        outputPorts.put(key, value);
    }

    /**
     * Registers a defined sub-routine on this interpreter.
     *
     * @param key the name of the defined sub-routine to register
     * @param value the defined sub-routine to register
     */
    public void register(String key, DefinitionProcess value) {
        definitions.put(key, value);
    }

    /**
     * Registers an <code>InputOperation</code> on this interpreter.
     *
     * @param key the name of the <code>InputOperation</code> to register
     * @param value the <code>InputOperation</code> to register
     */
    public void register(String key, InputOperation value) {
        inputOperations.put(key, value);
    }

    /**
     * Registers an <code>EmbeddedServiceLoader</code> on this interpreter.
     *
     * @param n the <code>EmbeddedServiceLoader</code> to register
     */
    public void addEmbeddedServiceLoader(EmbeddedServiceLoader n) {
        embeddedServiceLoaders.add(n);
    }

    /**
     * Returns the <code>EmbeddedServiceLoader</code> instances registered on
     * this interpreter.
     *
     * @return the <code>EmbeddedServiceLoader</code> instances registered on
     * this interpreter
     */
    public Collection< EmbeddedServiceLoader> embeddedServiceLoaders() {
        return embeddedServiceLoaders;
    }

    /**
     * Makes this <code>Interpreter</code> entering in exiting mode. When in
     * exiting mode, an interpreter waits for each session to finish its
     * execution and then terminates gracefully the execution of the entire
     * program. An interpreter in exiting mode cannot receive any more messages.
     *
     * Multiple calls of this method are redundant.
     *
     * The fact that the interpreter cannot receive any more messages after
     * entering exiting mode can cause deadlocks if a session is waiting for a
     * message to finish its execution. Use this method with caution.
     */
    public void exit() {
        exit(awaitTerminationTimeout);
    }

    /**
     * Makes this <code>Interpreter</code> entering in exiting mode. When in
     * exiting mode, an interpreter waits for each session to finish its
     * execution and then terminates gracefully the execution of the entire
     * program. An interpreter in exiting mode cannot receive any more messages.
     *
     * Multiple calls of this method are redundant.
     *
     * The fact that the interpreter cannot receive any more messages after
     * entering exiting mode can cause deadlocks if a session is waiting for a
     * message to finish its execution. Use this method with caution.
     *
     * @param terminationTimeout the timeout for the wait of the termination of
     * running processes
     */
    public void exit(long terminationTimeout) {
        synchronized (this) {
            if (exiting) {
                return;
            } else {
                exiting = true;
            }
        }
        exitingLock.lock();
        try {
            exitingCondition.signalAll();
        } finally {
            exitingLock.unlock();
        }
        timer.cancel();
        checkForExpiredTimeoutHandlers();
        processExecutorService.shutdown();
        nativeExecutorService.shutdown();
        timeoutHandlerExecutor.shutdown();
        commCore.shutdown();
        try {
            nativeExecutorService.awaitTermination(terminationTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        try {
            processExecutorService.awaitTermination(terminationTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        try {
            timeoutHandlerExecutor.awaitTermination(terminationTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        free();
    }

    /**
     * Returns <code>true</code> if this interpreter is in exiting mode,
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if this interpreter is in exiting mode,
     * <code>false</code> otherwise
     * @see #exit()
     */
    public boolean exiting() {
        return exiting;
    }

    /**
     * Logs an unhandled fault using the logger of this interpreter. This method
     * is used by sessions that had to terminate due to a fault which could not
     * be handled, due to a missing fault handler.
     *
     * @param fault the <code>FaultException</code> that could not be handled
     */
    public void logUnhandledFault(FaultException fault) {
        logInfo("Thrown unhandled fault: " + fault.faultName());
    }

    /**
     * Logs an information message using the logger of this interpreter.
     *
     * @param message the message to log
     */
    public void logInfo(String message) {
        log_LOGINFO(logPrefix + message);
        logger.info(logPrefix + message);
    }

    /**
     * Logs an information message using the logger of this interpreter (logger
     * level: fine).
     *
     * @param message the message to log
     */
    public void logFine(String message) {
        log_FINE(logPrefix + message);
        logger.fine(logPrefix + message);
    }

    /**
     * Logs an information message using the logger of this interpreter (logger
     * level: fine).
     *
     * @param t the <code>Throwable</code> object whose stack trace has to be
     * logged
     */
    public void logFine(Throwable t) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(bs));
        log_FINE(logPrefix + bs.toString());
        logger.fine(logPrefix + bs.toString());
    }

    /**
     * Logs a severe error message using the logger of this interpreter.
     *
     * @param message the message to log
     */
    public void logSevere(String message) {

        log_SEVERE(logPrefix + message);
        logger.severe(logPrefix + message);
    }

    /**
     * Logs a warning message using the logger of this interpreter.
     *
     * @param message the message to log
     */
    public void logWarning(String message) {
        log_WARNING(logPrefix + message);
        logger.warning(logPrefix + message);
    }

    /**
     * Logs a severe error message, created by reading the stack trace of the
     * passed <code>Throwable</code>, using the logger of this interpreter.
     *
     * @param t the <code>Throwable</code> object whose stack trace has to be
     * logged
     */
    public void logSevere(Throwable t) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(bs));
        log_SEVERE(bs.toString());
        logger.severe(logPrefix + bs.toString());
    }

    /**
     * Logs a warning message, created by reading the stack trace of the passed
     * <code>Throwable</code>, using the logger of this interpreter.
     *
     * @param t the <code>Throwable</code> object whose stack trace has to be
     * logged
     */
    public void logWarning(Throwable t) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(bs));
        log_WARNING(logPrefix + bs.toString());
        logger.warning(logPrefix + bs.toString());
    }

    /**
     * Returns the execution mode of this Interpreter.
     *
     * @return the execution mode of this Interpreter
     * @see Constants.ExecutionMode
     */
    public Constants.ExecutionMode executionMode() {
        return executionMode;
    }

    /**
     * Sets the execution mode of this Interpreter.
     *
     * @param mode the execution mode to set
     * @see Constants.ExecutionMode
     */
    public void setExecutionMode(Constants.ExecutionMode mode) {
        executionMode = mode;
    }

    /**
     * Adds a correlation set to this interpreter.
     *
     * @param set the correlation set to add
     */
    public void addCorrelationSet(CorrelationSet set) {
        correlationSets.add(set);
        for (String operation : set.correlatingOperations()) {
            operationCorrelationSetMap.put(operation, set);
        }
    }

    public CorrelationSet getCorrelationSetForOperation(String operationName) {
        return operationCorrelationSetMap.get(operationName);
    }

    /**
     * Returns the correlation sets of this Interpreter.
     *
     * @return the correlation sets of this Interpreter
     */
    public List< CorrelationSet> correlationSets() {
        return correlationSets;
    }

    /**
     * Returns the Interpreter the current thread is referring to.
     *
     * @return the Interpreter the current thread is referring to
     */
    public static Interpreter getInstance() {
        Thread t = Thread.currentThread();
        if (t instanceof InterpreterThread) {
            return ((InterpreterThread) t).interpreter();
        }
        return null;
    }

    /**
     * Returns the JolieClassLoader this Interpreter is using.
     *
     * @return the JolieClassLoader this Interpreter is using
     */
    public JolieClassLoader getClassLoader() {
        return classLoader;
    }

    private void log_LOGINFO(String message) {
        final Tracer tracer = Interpreter.getInstance().tracer();
        tracer.trace(() -> new ErrorTraceAction(
                ExecutionThread.currentThread().getSessionId(),
                ErrorTraceAction.Type.ERROR_LOGINFO,
                message,
                System.currentTimeMillis()
        ));
    }

    private void log_WARNING(String message) {
        final Tracer tracer = Interpreter.getInstance().tracer();
        tracer.trace(() -> new ErrorTraceAction(
                ExecutionThread.currentThread().getSessionId(),
                ErrorTraceAction.Type.ERROR_WARNING,
                message,
                System.currentTimeMillis()
        ));
    }

    private void log_FINE(String message) {
        final Tracer tracer = Interpreter.getInstance().tracer();
        tracer.trace(() -> new ErrorTraceAction(
                ExecutionThread.currentThread().getSessionId(),
                ErrorTraceAction.Type.ERROR_FINE,
                message,
                System.currentTimeMillis()
        ));
    }

    private void log_SEVERE(String message) {
        final Tracer tracer = Interpreter.getInstance().tracer();
        tracer.trace(() -> new ErrorTraceAction(
                ExecutionThread.currentThread().getSessionId(),
                ErrorTraceAction.Type.ERROR_SEVERE,
                message,
                System.currentTimeMillis()
        ));
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns <code>true</code> if this interpreter is in verbose mode.
     *
     * @return <code>true</code> if this interpreter is in verbose mode
     */
    /* public boolean verbose()
     {
     return verbose;
     } */
    /**
     * Constructor.
     *
     * @param args The command line arguments.
     * @param parentClassLoader the parent ClassLoader to fall back when not
     * finding resources.
     * @param programDirectory the program directory of this Interpreter,
     * necessary if it is run inside a JAP file.
     * @throws CommandLineException if the command line is not valid or asks for
     * simple information. (like --help and --version)
     * @throws FileNotFoundException if one of the passed input files is not
     * found.
     * @throws IOException if a Scanner constructor signals an error.
     */
    public Interpreter(long timestamp, String[] args, ClassLoader parentClassLoader, File programDirectory)
            throws CommandLineException, FileNotFoundException, IOException {
        this(timestamp, args, parentClassLoader, programDirectory, false);
    }

    public Interpreter(long timestamp, String[] args, ClassLoader parentClassLoader, File programDirectory, boolean ignoreFile)
            throws CommandLineException, FileNotFoundException, IOException {
        this.timestamp = timestamp;
        this.parentClassLoader = parentClassLoader;

        cmdParser = new CommandLineParser(args, parentClassLoader, ignoreFile);
        classLoader = cmdParser.jolieClassLoader();
        optionArgs = cmdParser.optionArgs();
        programFilename = cmdParser.programFilepath().getName();
        arguments = cmdParser.arguments();

        this.correlationEngine = cmdParser.correlationAlgorithmType().createInstance(this);

        commCore = new CommCore(this, cmdParser.connectionsLimit() /*, cmdParser.connectionsCache() */);
        includePaths = cmdParser.includePaths();

        StringBuilder builder = new StringBuilder();
        builder.append('[');
        builder.append(programFilename);
        builder.append("] ");
        logPrefix = builder.toString();

        if (cmdParser.tracer()) {
            tracer = new PrintingTracer(this);
        } else if (cmdParser.jTracer()) {
            tracer = classLoader.createTracerFactory("jolie.jtracer.JTracer", this, programFilename, timestamp);
        } else {
            tracer = new DummyTracer();
        }

        logger.setLevel(cmdParser.logLevel());

        timer = new Timer(programFilename + "-Timer");
        exitingLock = new ReentrantLock();
        exitingCondition = exitingLock.newCondition();

        if (cmdParser.programDirectory() == null) {
            this.programDirectory = programDirectory;
        } else {
            this.programDirectory = cmdParser.programDirectory();
        }
        if (this.programDirectory == null) {
            throw new IOException("Could not localize the service execution directory. This is probably a bug in the JOLIE interpreter, please report it to jolie-devel@lists.sf.net");
        }
    }

    /**
     * Constructor.
     *
     * @param args The command line arguments.
     * @param parentClassLoader the parent ClassLoader to fall back when not
     * finding resources.
     * @param programDirectory the program directory of this Interpreter,
     * necessary if it is run inside a JAP file.
     * @param internalServiceProgram
     * @throws CommandLineException if the command line is not valid or asks for
     * simple information. (like --help and --version)
     * @throws FileNotFoundException if one of the passed input files is not
     * found.
     * @throws IOException if a Scanner constructor signals an error.
     */
    public Interpreter(long timestamp, String[] args, ClassLoader parentClassLoader, File programDirectory, Interpreter parentInterpreter, Program internalServiceProgram)
            throws CommandLineException, FileNotFoundException, IOException {
        this(timestamp, args, parentClassLoader, programDirectory, true);

        this.parentInterpreter = parentInterpreter;
        this.internalServiceProgram = internalServiceProgram;
    }

    /**
     * Returns the parent directory of the program executed by this Interpreter.
     *
     * @return the parent directory of the program executed by this Interpreter.
     */
    public File programDirectory() {
        return programDirectory;
    }

    public Interpreter parentInterpreter() {
        return parentInterpreter;
    }

    /**
     * Returns the program filename this interpreter was launched with.
     *
     * @return the program filename this interpreter was launched with
     */
    public String programFilename() {
        return programFilename;
    }

    /**
     * Returns the parent class loader passed to the constructor of this
     * interpreter.
     *
     * @return the parent class loader passed to the constructor of this
     * interpreter
     */
    public ClassLoader parentClassLoader() {
        return parentClassLoader;
    }

    /**
     * Returns the global lock registered on this interpreter with the passed
     * identifier. If a global lock with such identifier is not registered, a
     * new one is automatically created, registered and returned.
     *
     * @param id the global lock identifier
     * @return the global lock registered on this interpreter with the specified
     * identifier
     */
    public Object getLock(String id) {
        Object l = locksMap.get(id);
        if (l == null) {
            l = new Object();
            locksMap.put(id, l);
        }
        return l;
    }

    public SessionStarter getSessionStarter(String operationName) {
        return sessionStarters.get(operationName);
    }

    /**
     * Returns the {@code global} value of this Interpreter.
     *
     * @return the {@code global} value of this Interpreter
     */
    public Value globalValue() {
        return globalValue;
    }

    private InitSessionThread initExecutionThread;
    private SessionThread mainSession = null;
    private final Queue< SessionThread> waitingSessionThreads = new LinkedList<>();

    /**
     * Returns the {@link SessionThread} of the Interpreter that started the
     * program execution.
     *
     * @return the {@link SessionThread} of the Interpreter that started the
     * program execution
     */
    public SessionThread initThread() {
        return initExecutionThread;
    }

    private static class InterpreterStartFuture implements Future< Exception> {

        private final CountDownLatch cl = new CountDownLatch(1);
        private Exception result;

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public Exception get(long timeout, TimeUnit unit)
                throws InterruptedException, TimeoutException {
            if (!cl.await(timeout, unit)) {
                throw new TimeoutException();
            }
            return result;
        }

        public Exception get()
                throws InterruptedException {
            cl.await();
            return result;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return cl.getCount() == 0;
        }

        private void setResult(Exception e) {
            result = e;
            cl.countDown();
        }
    }

    /**
     * Starts this interpreter, returning a <code>Future</code> which can be
     * interrogated to know when the interpreter start procedure has been
     * completed and the interpreter is ready to receive messages.
     *
     * @return a <code>Future</code> which can be interrogated to know when the
     * interpreter start procedure has been completed and the interpreter is
     * ready to receive messages.
     */
    public Future< Exception> start() {
        InterpreterStartFuture f = new InterpreterStartFuture();
        (new StarterThread(f)).start();
        return f;
    }

    private void init()
            throws InterpreterException, IOException {
        /**
         * Order is important. 1 - CommCore needs the OOIT to be initialized. 2
         * - initExec must be instantiated before we can receive communications.
         */
        if (buildOOIT() == false && !check) {
            throw new InterpreterException("Error: the interpretation environment couldn't have been initialized");
        }
        if (check) {
            exit();
        } else {
            sessionStarters = Collections.unmodifiableMap(sessionStarters);
            try {
                initExecutionThread = new InitSessionThread(this, getDefinition("init"));

                commCore.init();

                // Initialize program arguments in the args variabile.
                ValueVector jArgs = ValueVector.create();
                for (String s : arguments) {
                    jArgs.add(Value.create(s));
                }
                initExecutionThread.state().root().getChildren("args").deepCopy(jArgs);
                /* initExecutionThread.addSessionListener( new SessionListener() {
                 public void onSessionExecuted( SessionThread session )
                 {}
                 public void onSessionError( SessionThread session, FaultException fault )
                 {
                 exit();
                 }
                 }); */

                correlationEngine.onSingleExecutionSessionStart(initExecutionThread);
                // initExecutionThread.addSessionListener( correlationEngine );
                initExecutionThread.start();
            } catch (InvalidIdException e) {
                assert false;
            }
        }
    }

    private void runCode() {
        if (!check) {
            SessionThread t;
            synchronized (this) {
                t = initExecutionThread;
            }
            try {
                if (t != null) {
                    t.join();
                }
            } catch (InterruptedException e) {
                logSevere(e);
            }

            if (executionMode == Constants.ExecutionMode.SINGLE) {
                synchronized (this) {
                    t = mainSession;
                }
                try {
                    if (t != null) {
                        mainSession.start();
                        mainSession.join();
                    }
                } catch (InterruptedException e) {
                    logSevere(e);
                }
            } else {
                exitingLock.lock();
                try {
                    exitingCondition.await();
                } catch (InterruptedException e) {
                    logSevere(e);
                } finally {
                    exitingLock.unlock();
                }
            }
        }
    }

    /**
     * Runs the interpreter behaviour specified by command line. The default
     * behaviour is to execute the input code.
     *
     * Note that you must shutdown the CommCore of this Interpreter manually
     * after calling this method.
     *
     * @throws IOException if a Parser propagates a Scanner exception
     * @throws InterpreterException if the interpretation tree could not be
     * built
     */
    public void run()
            throws InterpreterException, IOException {
        init();
        runCode();
    }

    private final ExecutorService nativeExecutorService
            = new JolieThreadPoolExecutor(new NativeJolieThreadFactory(this));
    // Executors.newCachedThreadPool( new NativeJolieThreadFactory( this ) );
    private final ExecutorService processExecutorService
            = new JolieThreadPoolExecutor(new JolieExecutionThreadFactory(this));
    // Executors.newCachedThreadPool( new JolieExecutionThreadFactory( this ) );

    /**
     * Runs an asynchronous task in this Interpreter internal thread pool.
     *
     * @param r the Runnable object to execute
     */
    public void execute(Runnable r) {
        nativeExecutorService.execute(r);
    }

    public Executor taskExecutor() {
        return nativeExecutorService;
    }

    public Future<?> runJolieThread(Runnable task) {
        return processExecutorService.submit(task);
    }

    private static final AtomicInteger starterThreadCounter = new AtomicInteger();

    private static String createStarterThreadName(String programFilename) {
        return programFilename + "-StarterThread-" + starterThreadCounter.incrementAndGet();
    }

    private class StarterThread extends Thread {

        private final InterpreterStartFuture future;

        public StarterThread(InterpreterStartFuture future) {
            super(createStarterThreadName(programFilename));
            this.future = future;
            setContextClassLoader(classLoader);
        }

        @Override
        public void run() {
            try {
                init();
                future.setResult(null);
            } catch (Exception e) {
                future.setResult(e);
            }
            runCode();
            //commCore.shutdown();
            // final boolean proceed;
            exit();
        }
    }

    private void free() {
        /* We help the Java(tm) Garbage Collector.
         * Looks like it needs this or the Interpreter
         * does not get collected.
         */
        definitions.clear();
        inputOperations.clear();
        locksMap.clear();
        initExecutionThread = null;
        sessionStarters = new HashMap<>();
        outputPorts.clear();
        correlationSets.clear();
        globalValue.erase();
        embeddedServiceLoaders.clear();
        classLoader = null;
        commCore = null;
        // System.gc();
    }

    /**
     * Returns the CommCore of this Interpreter.
     *
     * @return the CommCore of this Interpreter
     */
    public CommCore commCore() {
        return commCore;
    }

    private boolean buildOOIT()
            throws InterpreterException {
        try {
            Program program;
            if (cmdParser.isProgramCompiled()) {
                final ObjectInputStream istream = new ObjectInputStream(cmdParser.programStream());
                final Object o = istream.readObject();
                if (o instanceof Program) {
                    program = (Program) o;
                } else {
                    throw new InterpreterException("Input compiled program is not a JOLIE program");
                }
            } else {
                if (this.internalServiceProgram != null) {
                    program = this.internalServiceProgram;
                } else {
                    final OLParser olParser = new OLParser(new Scanner(cmdParser.programStream(), cmdParser.programFilepath().toURI(), cmdParser.charset()), includePaths, classLoader);

                    olParser.putConstants(cmdParser.definedConstants());
                    program = olParser.parse();
                }
                program = OLParseTreeOptimizer.optimize(program);
            }

            cmdParser.close();

            check = cmdParser.check();

            final SemanticVerifier semanticVerifier;

            if (check) {
                SemanticVerifier.Configuration conf = new SemanticVerifier.Configuration();
                conf.setCheckForMain(false);
                semanticVerifier = new SemanticVerifier(program, conf);
            } else {
                semanticVerifier = new SemanticVerifier(program);
            }

            try {
                semanticVerifier.validate();
            } catch (SemanticException e) {
                logger.severe(e.getErrorMessages());
                throw new InterpreterException("Exiting");
            }

            if (cmdParser.typeCheck()) {
                TypeChecker typeChecker = new TypeChecker(
                        program,
                        semanticVerifier.executionMode(),
                        semanticVerifier.correlationFunctionInfo()
                );
                if (!typeChecker.check()) {
                    throw new InterpreterException("Exiting");
                }
            }

            if (check) {
                return false;
            } else {
                return (new OOITBuilder(
                        this,
                        program,
                        semanticVerifier.isConstantMap(),
                        semanticVerifier.correlationFunctionInfo()))
                        .build();
            }

        } catch (IOException | ParserException | ClassNotFoundException e) {
            //log(new InterpreterException(e).getMessage());
            throw new InterpreterException(e);
        } finally {
            cmdParser = null; // Free memory
        }
    }

    /**
     * Starts a service session.
     *
     * @param message the message triggering the session start
     * @param channel the channel of the message triggering the session start
     * @return {@code true} if the service session is started, {@code false}
     * otherwise
     */
    public boolean startServiceSession(final CommMessage message, CommChannel channel) {
        if (executionMode == Constants.ExecutionMode.SINGLE) {
            return false;
        }

        SessionStarter starter = sessionStarters.get(message.operationName());
        if (starter == null) {
            return false;
        }

        try {
            initExecutionThread.join();
        } catch (InterruptedException e) {
            return false;
        }

        final SessionThread spawnedSession;

        if (executionMode == Constants.ExecutionMode.CONCURRENT) {
            State state = initExecutionThread.state().clone();
            jolie.process.Process sequence = new SequentialProcess(new jolie.process.Process[]{
                starter.guard.receiveMessage(new SessionMessage(message, channel), state),
                starter.body
            });
            spawnedSession = new SessionThread(
                    sequence, state, initExecutionThread
            );
            correlationEngine.onSessionStart(spawnedSession, starter, message);
            spawnedSession.addSessionListener(correlationEngine);
            logSessionStart(message.operationName(), spawnedSession.getSessionId(),
                    message.id(), message.value());
            spawnedSession.addSessionListener(new SessionListener() {
                public void onSessionExecuted(SessionThread session) {
                    logSessionEnd(message.operationName(), session.getSessionId());
                }

                public void onSessionError(SessionThread session, FaultException fault) {
                    logSessionEnd(message.operationName(), session.getSessionId());
                }
            });
            spawnedSession.start();
        } else if (executionMode == Constants.ExecutionMode.SEQUENTIAL) {
            /*
             * We use sessionThreads to handle sequential execution of spawn requests
             */
            State state = initExecutionThread.state().clone();
            jolie.process.Process sequence = new SequentialProcess(new jolie.process.Process[]{
                starter.guard.receiveMessage(new SessionMessage(message, channel), state),
                starter.body
            });
            spawnedSession = new SessionThread(
                    sequence, state, initExecutionThread
            );
            correlationEngine.onSessionStart(spawnedSession, starter, message);
            spawnedSession.addSessionListener(correlationEngine);
            spawnedSession.addSessionListener(new SessionListener() {
                public void onSessionExecuted(SessionThread session) {
                    synchronized (waitingSessionThreads) {
                        if (!waitingSessionThreads.isEmpty()) {
                            waitingSessionThreads.poll();
                            if (!waitingSessionThreads.isEmpty()) {
                                waitingSessionThreads.peek().start();
                            }
                        }
                    }
                    logSessionEnd(message.operationName(), session.getSessionId());
                }

                public void onSessionError(SessionThread session, FaultException fault) {
                    synchronized (waitingSessionThreads) {
                        if (!waitingSessionThreads.isEmpty()) {
                            waitingSessionThreads.poll();
                            if (!waitingSessionThreads.isEmpty()) {
                                waitingSessionThreads.peek().start();
                            }
                        }
                    }
                    logSessionEnd(message.operationName(), session.getSessionId());
                }
            });
            synchronized (waitingSessionThreads) {
                if (waitingSessionThreads.isEmpty()) {
                    waitingSessionThreads.add(spawnedSession);
                    waitingSessionThreads.peek().start();
                } else {
                    waitingSessionThreads.add(spawnedSession);
                }
            }
        }
        return true;
    }

    private void logSessionStart(String operationName, String sessionId, long messageId, Value message) {
        if (isMonitoring()) {
            fireMonitorEvent(new SessionStartedEvent(operationName, sessionId));
            fireMonitorEvent(new OperationStartedEvent(operationName, sessionId, Long.toString(messageId), message));
        }
    }

    private void logSessionEnd(String operationName, String sessionId) {
        if (isMonitoring()) {
            fireMonitorEvent(new SessionEndedEvent(operationName, sessionId));
        }
    }

    private final Map< String, EmbeddedServiceLoaderFactory> embeddingFactories = new ConcurrentHashMap<>();

    public EmbeddedServiceLoaderFactory getEmbeddedServiceLoaderFactory(String name)
            throws IOException {
        EmbeddedServiceLoaderFactory factory = embeddingFactories.get(name);
        if (factory == null) {
            factory = getClassLoader().createEmbeddedServiceLoaderFactory(name, this);
            if (factory != null) {
                embeddingFactories.put(name, factory);
            }
        }
        return factory;
    }
}
