/*
 * #%L
 * DiMaWo
 * %%
 * Copyright (C) 2011 DiMaWo Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package dimawo.agents;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import dimawo.agents.events.StopAgent;



/**
 * @author Gerard Dethier
 * 
 * An agent is a component that handles sequentially messages
 * submitted in an asynchronous way.
 * The message handling code is executed by a single
 * thread. Messages can be submitted by different threads (message
 * submission is a thread-safe operation), including the one executing the
 * handling code.
 * <p>
 * Agents are well suited to the design of complex parallel applications
 * involving mostly asynchronous operations. The behavior of an application
 * composed of agents emerges from the exchange of messages in an asynchronous
 * way between the agents of the application. Agent based programming (ABP) can
 * be seen as an asynchronous form of object-oriented programming (OOP):
 * the execution of the application is based on agents (objects) exchanging
 * messages (method calls). The main difference between the 2 approaches is that
 * in ABP, calls are asynchronous (a call does not immediately generate a
 * result).
 * <p>
 * This class represents an asynchronous agent template. Messages submitted to
 * an instance of this class are inserted into a blocking queue by
 * {@link #submitMessage(Object) submitMessage}. This method is protected so
 * subclasses of AbstractAgent are able to statically (i.e. at compile time)
 * "control" the type of the messages they accept (for example,
 * only {@link java.lang.String String} objects) by defining particular
 * submission methods (for example, submitString(String)).
 * <p>
 * An asynchronous agent can be in 3 states: init, running and stopped. Init
 * state indicates the agent has been instantiated but has not yet been started
 * (i.e. it does not yet handle messages, though these can already be inserted
 * into message queue). Running state implies the agent is currently handling
 * messages. Finally, stopped state means the agent has been stopped and no more
 * messages are going to be handled.
 * <p>
 * The agent is in its initial state after it has been instantiated. An agent
 * changes to running state after a call to {@link #start() start}
 * method. A call to {@link #start() start} starts the thread that will
 * execute message handling code (see {@link #run() run}).
 * Before any message is handled, the {@link #init() init} method is
 * called (this method must be defined by a subclass of AbstractAgent).
 * After that, the message handling code extracts each message from
 * queue and handles it according to its type
 * (see {@link #messageHandling() messageHandling} method).
 * <p>
 * If {@link #init() init} throws a Throwable, the agent changes to stopped state and no
 * message is handled.
 * Stopped state can also be reached if the handling of a message causes a
 * Throwable to be thrown. Finally, a call to {@link #stop() stop} inserts a
 * special message into queue. The handling of this message causes the agent
 * to move to stopped state. This implies that the effect of a call to
 * {@link #stop() stop} is not immediate.
 * <p>
 * As soon as an AbstractAgent reaches stopped state, {@link #exit() exit} method
 * is executed and after that, the thread executing the message handling code
 * finishes its execution.
 * <p>
 * When stopped state was reached because a Throwable was thrown by either
 * {@link #init() init} or {@link #messageHandling() messageHandling}, the error
 * is forwarded to an {@link dimawo.agents.ErrorHandler}, if any was set,
 * before {@link #exit() exit} is called.
 * <p>
 * An AbstractAgent is also an ErrorHandler:
 * {@link #signalChildError(Throwable, String) signalChildError} inserts a message
 * into queue. A default behaviour for this message handling is implemented by
 * {@link #handleChildException(UncaughtThrowable)}. Of course, This method can
 * be overriden.
 * <p>
 * Finally, an AbstractAgent has a particular logging interface: by default,
 * all messages printed using
 * {@link #agentPrintMessage(int, String) agentPrintMessage(int, String)}
 * or {@link #agentPrintMessage(Throwable) agentPrintMessage(Throwable)}
 * are printed onto standard output (System.out). However, agent's logging
 * stream can be changed by {@link #setPrintStream(PrintStream) setPrintStream}.
 * <p>
 * A name can be associated to an agent, this name is used by logging
 * and is used to name the thread running the message handling code.
 */
public abstract class AbstractAgent
implements Runnable, ErrorHandler {
	
	/** Incoming messages queue. */
	private LinkedBlockingQueue<Object> incoming;
	
	/** Message handling loop thread. */
	transient private Thread agentThread;

	/** Represents the 3 possible states of an AbstractAgent */
	public enum AgentState {
		/** Init state (see {@link asynchronousAgents.AbstractAgent
		 * AbstractAgent} behavior). */
		INIT,
		/** Running state (see {@link asynchronousAgents.AbstractAgent
		 * AbstractAgent} behavior). */
		RUNNING,
		/** Stopped state (see {@link asynchronousAgents.AbstractAgent
		 * AbstractAgent} behavior). */
		STOPPED
	};
	/** The current state of the agent. */
	private AgentState state;
	/** The throwable that was thrown initially after agent was started by
	 * {@link #init() init} or during message handling
	 * {@link #messageHandling() messageHandling}. */
	private Throwable error;


	/** The name of the agent (used for logging and thread name). */
	protected String agentName;
	/** The output stream logs are printed onto. */
	private PrintStream printStream;

	/** Error handler associated to this agent. */
	protected ErrorHandler errorHandler;
	
	/** Verbosity level of this agent.
	 * See {@link #agentPrintMessage(int, String) agentPrintMessage}. */
	private int currentVerbosityLevel;
	/**
	 * Default verbosity level associated to a newly instantiated agent.
	 * See {@link #agentPrintMessage(int, String) agentPrintMessage}.
	 */
	private static int defaultVerbosityLevel = 0;


	/**
	 * Instantiates an <code>AbstractAgent</code> without error handler and
	 * name. The message handling thread is not a daemon and the capacity
	 * of message queue is not limited.
	 * This constructor is equivalent to
	 * {@link #AbstractAgent(ErrorHandler, String)}
	 * constructor called with the 2 arguments having <code>null</code> as value.
	 */
	public AbstractAgent() {
		this(null, "");
	}
	
	/**
	 * Instantiates an <code>AbstractAgent</code> having given error handler and
	 * name. The message handling thread is not a daemon and the capacity
	 * of message queue is not limited.
	 * This constructor is equivalent to
	 * {@link #AbstractAgent(ErrorHandler, String, boolean)}
	 * constructor called with <code>AbstractAgent(parent, name, false)</code>.
	 * 
	 * @param parent The error handler to associate to this agent.
	 * @param name The name to associate to this agent.
	 * 
	 * @see ErrorHandler
	 */
	public AbstractAgent(ErrorHandler parent, String name) {
		this(parent, name, false);
	}
	
	/**
	 * Instantiates an <code>AbstractAgent</code> having given error handler and
	 * name. The capacity of message queue is not limited.
	 * This constructor is equivalent to
	 * {@link #AbstractAgent(ErrorHandler, String, boolean, int)}
	 * constructor called with <code>AbstractAgent(parent, name, daemon, 0)</code>.
	 * 
	 * @param parent The ErrorHandler to associate to this agent.
	 * @param name The name to associate to this agent.
	 * @param daemon Daemon flag for the thread that will execute message
	 * handling code.
	 * 
	 * @see ErrorHandler
	 */
	public AbstractAgent(ErrorHandler parent, String name, boolean daemon) {
		this(parent, name, daemon, 0);
	}
	
	/**
	 * Instantiates an <code>AbstractAgent</code> having given error handler and
	 * name. The message handling thread is not a daemon.
	 * This constructor is equivalent to
	 * {@link #AbstractAgent(ErrorHandler, String, boolean, int)}
	 * constructor called with <code>AbstractAgent(parent, name, false, capacity)</code>.
	 * 
	 * @param parent The ErrorHandler to associate to this agent.
	 * @param name The name to associate to this agent.
	 * @param capacity The capacity of the incoming messages queue
	 * (see {@link #submitMessage(Object) submitMessage}).
	 * 
	 * @see ErrorHandler
	 */
	public AbstractAgent(ErrorHandler parent, String name, int capacity) {
		this(parent, name, false, capacity);
	}

	/**
	 * Instantiates an <code>AbstractAgent</code> having given error handler and
	 * name.
	 * 
	 * @param parent The <code>ErrorHandler</code> to associate to this agent.
	 * @param name The name to associate to this agent.
	 * @param daemon Daemon flag for the thread that will execute message
	 * handling code.
	 * @param capacity The capacity of the incoming messages queue
	 * (see {@link #submitMessage(Object) submitMessage}).
	 * 
	 * @see ErrorHandler
	 */
	public AbstractAgent(ErrorHandler parent, String name, boolean daemon,
			int capacity) {
		if(capacity <= 0)
			incoming = new LinkedBlockingQueue<Object>();
		else
			incoming = new LinkedBlockingQueue<Object>(capacity);
		
		agentName = name;
		currentVerbosityLevel = defaultVerbosityLevel;
		
		this.printStream = System.out;
		this.errorHandler = parent;
		
		agentThread = new Thread(this, name);
		agentThread.setDaemon(daemon);
		state = AgentState.INIT;
	}
	
	
	/**
	 * Sets the <code>ErrorHandler</code> associated to this agent.
	 * This method must be called before the agent is started (i.e.
	 * before a call to {@link #start()} start).
	 * 
	 * @param err The ErrorHandler.
	 * 
	 * @see ErrorHandler
	 */
	public void setErrorHandler(ErrorHandler err) {
		this.errorHandler = err;
	}
	
	/**
	 * Sets the daemon flag of the thread that will execute message
	 * handling code. This method must be called before the agent is started
	 * (i.e. before a call to {@link #start()}).
	 * 
	 * @param on Value of the daemon flag.
	 * 
	 * @see Thread#setDaemon(boolean)
	 */
	public void setDaemon(boolean on) {
		agentThread.setDaemon(on);
	}

	/**
	 * Sets the default verbosity level for all agents that will be instantiated
	 * after this call.
	 * 
	 * @param verbLevel A verbosity level (see
	 * {@link #agentPrintMessage(int, String)} agentPrintMessage).
	 */
	public static void setDefaultVerbosityLevel(int verbLevel) {
		defaultVerbosityLevel = verbLevel;
	}

	/**
	 * Returns the default verbosity level of agents.
	 * 
	 * @return The default verbosity level of agents.
	 */
	public static int getDefaultVerbosityLevel() {
		return defaultVerbosityLevel;
	}
	
	/**
	 * Sets the verbosity level for this agent.
	 * 
	 * @param verbLevel A verbosity level (see
	 * {@link #agentPrintMessage(int, String)} agentPrintMessage).
	 */
	public void setVerbosityLevel(int verbLevel) {
		currentVerbosityLevel = verbLevel;
	}
	
	/**
	 * Returns the verbosity level of this agent.
	 * 
	 * @return The verbosity level of this agent.
	 */
	public int getVerbosityLevel() {
		return currentVerbosityLevel;
	}

	/**
	 * Inserts a StopAgent event in the queue of this agent.
	 * If this method is called multiple times, only the first call will have an effect. The following calls will be ignored.
	 * This method ensures that the agent thread is interrupted only once.
	 * 
	 * @throws InterruptedException If the agent thread was interrupted.
	 * @throws AgentException If the agent is already stopped.
	 */
	public synchronized void stop() throws InterruptedException, AgentException {
		if(state.equals(AgentState.RUNNING)) {
			incoming.put(new StopAgent());
		} else {
			throw new AgentException("Agent cannot be stopped: "+state);
		}
	}
	
	/**
	 * Starts this agent. If no thread is attached to this agent or the agent was already stopped,
	 * this method will have no effect.
	 * 
	 * @throws AgentException If the agent was already started. 
	 */
	public synchronized void start() throws AgentException {
		if(state.equals(AgentState.INIT)) {
			state = AgentState.RUNNING;

			agentThread.start();
		} else {
			throw new AgentException("Agent cannot be started: "+state);
		}
	}
	
	
	/**
	 * Sets the state of the agent. This method allows the thread-safe update
	 * of the state of the agent.
	 * 
	 * @param newState The new state of the agent.
	 */
	private synchronized void setState(AgentState newState) {
		state = newState;
	}
	
	
	/**
	 * Provides the current state of the agent. This method allows the
	 * thread-safe access to the state of the agent.
	 * 
	 * @return The current state of the agent.
	 */
	public synchronized AgentState getState() {
		return state;
	}

	/**
	 * Waits until the agent's thread is stopped. If the agent was not already started, this method will have no effect.
	 * 
	 * @throws InterruptedException if this thread is interrupted.
	 * @throws AgentException 
	 */
	public void join() throws InterruptedException {
		agentThread.join();
	}
	
	/**
	 * Waits until the agent's thread is stopped or a time-out occurs.
	 * If the agent was not already started, this method will have no effect.
	 * 
	 * @param millis The duration of the time-out given in milliseconds.
	 * 
	 * @throws InterruptedException if this thread is interrupted.
	 */
	public void join(long millis) throws InterruptedException {
		agentThread.join(millis);
	}


	/**
	 * This method implements the <code>Runnable</code> interface. It
	 * describes the sequential code executed by message handling thread:
	 * <ol>
	 * <li>initialization,</li>
	 * <li>message handling loop,</li>
	 * <li>termination (caused by a stop event or an error).</li>
	 * </ol>
	 */
	@Override
	public void run() {

		// Init
		try {
			init();
		} catch (Throwable e) {
			error = e;
			setState(AgentState.STOPPED);
		}


		// Message handling
		try {
			AgentState currentState = getState();
			while( ! currentState.equals(AgentState.STOPPED)) {
				messageHandling();
				currentState = getState();
			}
		} catch (Throwable t) {
			error = t;
			setState(AgentState.STOPPED);
		}

		// Exit
		if(error != null) {
			agentPrintMessage(error);
			if(errorHandler != null)
				errorHandler.signalChildError(error, agentName);
		}

		exit();
	}

	
	/**
	 * Inserts an error event in message queue. This method is generally called
	 * by child agents i.e. agents instantiated by this agent.
	 */
	@Override
	public void signalChildError(Throwable t, String childName) {
		try {
			submitMessage(new UncaughtThrowable(t, childName));
		} catch (InterruptedException e) {
			agentPrintMessage(e);
		}
	}
	
	
	/**
	 * This method tries to take a message from messages queue.
	 * If no message is available, the method blocks until a message
	 * is inserted.
	 * 
	 * @throws Throwable If the handling of a message generated an error. 
	 * 
	 */
	private void messageHandling() throws Throwable {
		Object o = incoming.take();
		if(o instanceof StopAgent) {
			agentPrintMessage("StopAgent event taken from queue.");
			setState(AgentState.STOPPED);
		} else if(o instanceof UncaughtThrowable) {
			handleChildException((UncaughtThrowable) o);
		} else if(o instanceof Throwable) {
			throw (Throwable) o;
		} else {
			handleMessage(o);
		}
	}

	/**
	 * Queues a message in messages queue. The call can be blocking if
	 * the messages queue of the agent is full.
	 * 
	 * @param o The message to insert into the queue.
	 * 
	 * @throws InterruptedException If the call is blocking and executing
	 * thread is interrupted.
	 */
	protected void submitMessage(Object o) throws InterruptedException {
		incoming.put(o);
	}
	
	
	/**
	 * Handles an error event taken from message queue.
	 * 
	 * @param o The error.
	 * 
	 * @throws AgentException If the agent does not have an error handler.
	 */
	protected void handleChildException(UncaughtThrowable o)
	throws AgentException {
		if(errorHandler != null) {
			agentPrintMessage("Forwarding error to parent error handler");
			errorHandler.signalChildError(o, this.getClass().toString());
		} else {
			throw new AgentException("Unhandled child throwable and" +
					" no parent to forward to.", o.getCause());
		}
	}

	
	/**
	 * Logs a message on standard output if given verbosity level is high
	 * enough. The message is printed only if given verbosity level is greater
	 * or equal to current verbosity level.
	 * 
	 * @param verbLevel Message's verbosity level
	 * @param message The message to log.
	 * 
	 * @see #setVerbosityLevel(int)
	 */
	public void agentPrintMessage(int verbLevel, String message) {
		if(agentName == null)
			throw new Error("Agent name not set");
		if(verbLevel >= currentVerbosityLevel)
			printStream.println("["+agentName+"] "+message);
	}
	
	
	/**
	 * Logs a message on standard output if current verbosity level is lesser
	 * or equal to 0.
	 * 
	 * @param message The message to log.
	 * 
	 * @see #setVerbosityLevel(int)
	 */
	public void agentPrintMessage(String message) {
		agentPrintMessage(0, message);
	}

	
	/**
	 * Logs the stack trace of given <code>Throwable</code> instance on
	 * standard output.
	 * 
	 * @param e The given <code>Throwable</code> instance.
	 */
	public void agentPrintMessage(Throwable e) {
		e.printStackTrace(printStream);
	}


	/**
	 * Lists messages from messages queue and clears it. This allows an agent
	 * to handle the messages still present in its message queue when it
	 * terminates its execution (i.e. arrives in STOPPED state).
	 * 
	 * @return The list of messages from message queue.
	 */
	protected LinkedList<Object> flushPendingMessages() {
		LinkedList<Object> list = new LinkedList<Object>();
		while(incoming.size() > 0) {
			Object o = incoming.poll();
			list.add(o);
		}
		return list;
	}
	
	
	/**
	 * Sets the default print stream this agent logs to.
	 * The access to the print stream is not thread-safe.
	 * 
	 * @param printStream A print stream.
	 */
	protected void setPrintStream(PrintStream printStream) {
		this.printStream = printStream;
	}


	/**
	 * Sets the name of this agent.
	 * 
	 * @param name The name of the agent.
	 */
	protected void setAgentName(String name) {
		this.agentName = name;
		agentThread.setName(name);
	}

	
	/**
	 * Inserts a <code>Throwable</code> into message queue. When the
	 * <code>Throwable</code> is taken from queue, it is thrown and causes
	 * therefore the agent to terminate its execution.
	 * 
	 * @param t The <code>Throwable</code>.
	 * 
	 * @see #messageHandling()
	 * @see #run()
	 */
	protected void submitError(Throwable t) {
		try {
			submitMessage(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Implements the operations to be executed when agent enters INIT state.
	 * 
	 * @throws Throwable If an error occurred during initialization.
	 */
	protected abstract void init() throws Throwable;

	
	/**
	 * Handles a new message taken from the queue.
	 * 
	 * @param o A message.
	 * 
	 * @throws Throwable If an error occurred during message handling.
	 */
	protected abstract void handleMessage(Object o) throws Throwable;
	
	
	/**
	 * Implements the operations to be executed when agent enters STOPPED state.
	 * The agent enters this state because a stop event was taken from
	 * queue or an error occurred. The 2 situations can be differentiated by
	 * checking agent's state.
	 */
	protected abstract void exit();
}
