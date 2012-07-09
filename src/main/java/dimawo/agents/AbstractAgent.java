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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import dimawo.agents.events.InitAgent;
import dimawo.agents.events.StopAgent;

/**
 * @author Gerard Dethier
 * 
 *         An agent is a component that handles sequentially messages submitted
 *         in an asynchronous way. The message handling code is executed by a
 *         single thread. Messages can be submitted by different threads
 *         (message submission is a thread-safe operation), including the one
 *         executing the handling code.
 *         <p>
 *         Agents are well suited to the design of complex parallel applications
 *         involving mostly asynchronous operations. The behavior of an
 *         application composed of agents emerges from the exchange of messages
 *         in an asynchronous way between the agents of the application. Agent
 *         based programming (ABP) can be seen as an asynchronous form of
 *         object-oriented programming (OOP): the execution of the application
 *         is based on agents (objects) exchanging messages (method calls). The
 *         main difference between the 2 approaches is that in ABP, calls are
 *         asynchronous (a call does not immediately generate a result).
 *         <p>
 *         This class represents an asynchronous agent template. Messages
 *         submitted to an instance of this class are inserted into a blocking
 *         queue by {@link #submitMessage(Object) submitMessage}. This method is
 *         protected so subclasses of AbstractAgent are able to statically (i.e.
 *         at compile time) "control" the type of the messages they accept (for
 *         example, only {@link java.lang.String String} objects) by defining
 *         particular submission methods (for example, submitString(String)).
 *         <p>
 *         An asynchronous agent can be in 3 states: init, running and stopped.
 *         Init state indicates the agent has been instantiated but has not yet
 *         been started (i.e. it does not yet handle messages, though these can
 *         already be inserted into message queue). Running state implies the
 *         agent is currently handling messages. Finally, stopped state means
 *         the agent has been stopped and no more messages are going to be
 *         handled.
 *         <p>
 *         The agent is in its initial state after it has been instantiated. An
 *         agent changes to running state after a call to {@link #start() start}
 *         method. A call to {@link #start() start} starts the thread that will
 *         execute message handling code (see {@link #run() run}). Before any
 *         message is handled, the {@link #init() init} method is called (this
 *         method must be defined by a subclass of AbstractAgent). After that,
 *         the message handling code extracts each message from queue and
 *         handles it according to its type (see {@link #messageHandling()
 *         messageHandling} method).
 *         <p>
 *         If {@link #init() init} throws a Throwable, the agent changes to
 *         stopped state and no message is handled. Stopped state can also be
 *         reached if the handling of a message causes a Throwable to be thrown.
 *         Finally, a call to {@link #stop() stop} inserts a special message
 *         into queue. The handling of this message causes the agent to move to
 *         stopped state. This implies that the effect of a call to
 *         {@link #stop() stop} is not immediate.
 *         <p>
 *         As soon as an AbstractAgent reaches stopped state, {@link #exit()
 *         exit} method is executed and after that, the thread executing the
 *         message handling code finishes its execution.
 *         <p>
 *         When stopped state was reached because a Throwable was thrown by
 *         either {@link #init() init} or {@link #messageHandling()
 *         messageHandling}, the error is forwarded to an
 *         {@link dimawo.agents.ErrorHandler}, if any was set, before
 *         {@link #exit() exit} is called.
 *         <p>
 *         An AbstractAgent is also an ErrorHandler:
 *         {@link #signalChildError(Throwable, String) signalChildError} inserts
 *         a message into queue. A default behaviour for this message handling
 *         is implemented by {@link #handleChildException(UncaughtThrowable)}.
 *         Of course, This method can be overriden.
 *         <p>
 *         Finally, an AbstractAgent has a particular logging interface: by
 *         default, all messages printed using
 *         {@link #agentPrintMessage(int, String) agentPrintMessage(int,
 *         String)} or {@link #agentPrintMessage(Throwable)
 *         agentPrintMessage(Throwable)} are printed onto standard output
 *         (System.out). However, agent's logging stream can be changed by
 *         {@link #setPrintStream(PrintStream) setPrintStream}.
 *         <p>
 *         A name can be associated to an agent, this name is used by logging
 *         and is used to name the thread running the message handling code.
 */
public abstract class AbstractAgent<State> implements ErrorHandler {

  /** Represents the 3 possible states of an AbstractAgent */
  public enum AgentStatus {
    /**
     * Init state (see {@link asynchronousAgents.AbstractAgent AbstractAgent}
     * behavior).
     */
    INIT,
    /**
     * Running state (see {@link asynchronousAgents.AbstractAgent AbstractAgent}
     * behavior).
     */
    RUNNING,
    /**
     * Stopped state (see {@link asynchronousAgents.AbstractAgent AbstractAgent}
     * behavior).
     */
    STOPPED
  }

  /** Message handlers map. */
  private final Map<Class<?>, MessageHandler<State, ?>> handlers;

  /** Init message handler. */
  private MessageHandler<State, InitAgent> initHandler;

  /** Exit message handler. */
  private MessageHandler<State, StopAgent> exitHandler;;

  /** The current state of the agent. */
  private AgentStatus status;
  private boolean started;
  private boolean stopped;
  private boolean setup;

  private State state;

  /**
   * The throwable that was thrown initially after agent was started by
   * {@link #init() init} or during message handling {@link #messageHandling()
   * messageHandling}.
   */
  private Throwable error;

  /** The name of the agent (used for logging and thread name). */
  private String agentName;

  /** Error handler associated to this agent. */
  protected ErrorHandler errorHandler;

  private Router router = Router.getDefaultRouter();

  /**
   * Instantiates an <code>AbstractAgent</code> without error handler and name.
   * The message handling thread is not a daemon and the capacity of message
   * queue is not limited. This constructor is equivalent to
   * {@link #AbstractAgent(ErrorHandler, String)} constructor called with the 2
   * arguments having <code>null</code> as value.
   */
  public AbstractAgent(State initialState) {
    this(initialState, null, null);
  }

  /**
   * Instantiates an <code>AbstractAgent</code> having given error handler and
   * name.
   * 
   * @param parent
   *          The <code>ErrorHandler</code> to associate to this agent.
   * @param name
   *          The name to associate to this agent.
   * @see ErrorHandler
   */
  public AbstractAgent(State initialState, ErrorHandler parent, String name) {
    handlers = new HashMap<Class<?>, MessageHandler<State, ?>>();

    agentName = (name != null && !name.isEmpty()) ? name : getClass()
        .getCanonicalName();

    this.errorHandler = parent;

    status = AgentStatus.INIT;
    state = initialState;
  }

  @SuppressWarnings("unchecked")
  public void consumeMessage(Object message) throws AgentException {

    // Check agent has not already encountered an error
    if (error != null) {
      throw new AgentException(
          "Agent has encountered an error and cannot continue its execution");
    }

    // Consume next message in queue
    @SuppressWarnings("rawtypes")
    MessageHandler handler = handlers.get(message.getClass());
    if (handler != null) {
      try {
        handler.handle(state, message);
      } catch (Exception e) {
        error = e;
        setStatus(AgentStatus.STOPPED);
      }
    } else {
      error = new UnknownAgentMessage(message);
      setStatus(AgentStatus.STOPPED);
    }
  }

  public Throwable getError() {
    return error;
  }

  public String getName() {
    return agentName;
  }

  public Router getRouter() {
    return router;
  }

  public State getState() {
    return state;
  }

  /**
   * Provides the current state of the agent. This method allows the thread-safe
   * access to the state of the agent.
   * 
   * @return The current state of the agent.
   */
  public synchronized AgentStatus getStatus() {
    return status;
  }

  /**
   * Handles an error event taken from message queue.
   * 
   * @param o
   *          The error.
   * 
   * @throws AgentException
   *           If the agent does not have an error handler.
   */
  protected void handleChildException(UncaughtThrowable o)
      throws AgentException {
    if (errorHandler != null) {
      errorHandler.signalChildError(o, this.getClass().toString());
    } else {
      throw new AgentException("Unhandled child throwable and"
          + " no parent to forward to.", o.getCause());
    }
  }

  /**
   * Waits until the agent's thread is stopped. If the agent was not already
   * started, this method will have no effect.
   * 
   * @throws InterruptedException
   *           if this thread is interrupted.
   * @throws AgentException
   */
  public abstract void join() throws InterruptedException;

  /**
   * Waits until the agent's thread is stopped or a time-out occurs. If the
   * agent was not already started, this method will have no effect.
   * 
   * @param millis
   *          The duration of the time-out given in milliseconds.
   * 
   * @throws InterruptedException
   *           if this thread is interrupted.
   */
  public abstract void join(long millis) throws InterruptedException;

  protected void logError(Exception e) {
    if (agentName != null) {
      Logger.getLogger(agentName).error(e.getMessage(), e);
    } else {
      Logger.getLogger(getClass()).error(e.getMessage(), e);
    }
  }

  protected abstract void onStart();

  protected abstract void onStop();

  public void registerExitHandler(MessageHandler<State, StopAgent> handler)
      throws AgentException {
    if (exitHandler != null) {
      throw new AgentException("An exit handler has already been registered");
    }
    exitHandler = handler;
  }

  public void registerHandler(Class<?> messageType,
      MessageHandler<State, ?> handler) throws AgentException {
    if (handlers.containsKey(messageType)) {
      throw new AgentException(
          "A handler is already registered for message type "
              + messageType.getName());
    }
    handlers.put(messageType, handler);
  }

  public void registerInitHandler(MessageHandler<State, InitAgent> handler)
      throws AgentException {
    if (initHandler != null) {
      throw new AgentException("An init handler has already been registered");
    }
    initHandler = handler;
  }

  private void registerPrivateHandlers() throws AgentException {
    registerHandler(InitAgent.class, new MessageHandler<State, InitAgent>() {
      @Override
      public void handle(State agentState, InitAgent message) throws Exception {
        setStatus(AgentStatus.RUNNING);

        if (initHandler != null) {
          initHandler.handle(agentState, message);
        }
      }
    });

    registerHandler(StopAgent.class, new MessageHandler<State, StopAgent>() {
      @Override
      public void handle(State agentState, StopAgent message) throws Exception {
        setStatus(AgentStatus.STOPPED);

        if (exitHandler != null) {
          exitHandler.handle(agentState, message);
        }
      }
    });

    registerHandler(UncaughtThrowable.class,
        new MessageHandler<State, UncaughtThrowable>() {
          @Override
          public void handle(State agentState, UncaughtThrowable message)
              throws Exception {
            handleChildException(message);
          }
        });

    registerHandler(Exception.class, new MessageHandler<State, Exception>() {
      @Override
      public void handle(State agentState, Exception message) throws Exception {
        throw message;
      }
    });
  }

  public void route(String dest, Object message) throws AgentException,
      InterruptedException {
    @SuppressWarnings("rawtypes")
    AbstractAgent agent = router.getAgent(dest);
    if (agent == null) {
      throw new AgentException("No registered agent with name '" + dest + "'");
    }

    agent.submitMessage(message);
  }

  /**
   * Sets the name of this agent.
   * 
   * @param name
   *          The name of the agent.
   */
  protected void setAgentName(String name) {
    this.agentName = name;
  }

  /**
   * Sets the <code>ErrorHandler</code> associated to this agent. This method
   * must be called before the agent is started (i.e. before a call to
   * {@link #start()} start).
   * 
   * @param err
   *          The ErrorHandler.
   * 
   * @see ErrorHandler
   */
  public void setErrorHandler(ErrorHandler err) {
    this.errorHandler = err;
  }

  public void setName(String name) {
    if (setup) {
      throw new RuntimeException("Agent has already been setup");
    }
    this.agentName = name;
  }

  public void setRouter(Router router) {
    this.router = router;
  }

  /**
   * Sets the state of the agent. This method allows the thread-safe update of
   * the state of the agent.
   * 
   * @param newState
   *          The new state of the agent.
   */
  private synchronized void setStatus(AgentStatus newState) {
    status = newState;
  }

  public void setup() throws AgentException {
    router.registerAgent(this);
    registerPrivateHandlers();
    setup = true;
  }

  /**
   * Inserts an error event in message queue. This method is generally called by
   * child agents i.e. agents instantiated by this agent.
   */
  @Override
  public void signalChildError(Throwable t, String childName) {
    try {
      submitMessage(new UncaughtThrowable(t, childName));
    } catch (InterruptedException e) {
    }
  }

  /**
   * Starts this agent. If no thread is attached to this agent or the agent was
   * already stopped, this method will have no effect.
   * 
   * @throws AgentException
   *           If the agent was already started.
   * @throws InterruptedException
   */
  public synchronized void start() throws AgentException, InterruptedException {
    if (!setup) {
      throw new AgentException("setup() has not been invoked, cannot start");
    }

    if (!started) {
      started = true;
      submitMessage(new InitAgent());
      onStart();
    } else {
      throw new AgentException("Agent was already started");
    }
  }

  /**
   * Inserts a StopAgent event in the queue of this agent. If this method is
   * called multiple times, only the first call will have an effect. The
   * following calls will be ignored. This method ensures that the agent thread
   * is interrupted only once.
   * 
   * @throws InterruptedException
   *           If the agent thread was interrupted.
   * @throws AgentException
   *           If the agent is already stopped.
   */
  public synchronized void stop() throws InterruptedException, AgentException {
    if (!started) {
      throw new AgentException("Cannot stop agent that has not been started");
    }

    if (!stopped) {
      stopped = true;
      submitMessage(new StopAgent());
      onStop();
    } else {
      throw new AgentException("Agent has already been stopped");
    }
  }

  public void stopWithError(Exception error) throws InterruptedException {
    submitMessage(error);
  }

  /**
   * Queues a message in messages queue. The call can be blocking if the
   * messages queue of the agent is full.
   * 
   * @param o
   *          The message to insert into the queue.
   * 
   * @throws InterruptedException
   *           If the call is blocking and executing thread is interrupted.
   */
  public abstract void submitMessage(Object o) throws InterruptedException;
}
