package dimawo.agents;

public class ThreadAgent<State> extends AbstractAgent<State> implements
    Runnable {

  private Thread agentThread;

  public ThreadAgent(State initialState) {
    this(initialState, null, "");
  }

  public ThreadAgent(State initialState, ErrorHandler parent, String name) {
    this(initialState, parent, name, false);
  }

  public ThreadAgent(State initialState, ErrorHandler parent, String name,
      boolean daemon) {
    this(initialState, parent, name, daemon, 0);
  }

  public ThreadAgent(State initialState, ErrorHandler parent, String name,
      boolean daemon, int capacity) {
    super(initialState, parent, name, daemon, capacity);
    name = name != null ? name : getClass().getSimpleName();
    agentThread = new Thread(this, name);
  }

  public ThreadAgent(State initialState, ErrorHandler parent, String name,
      int capacity) {
    super(initialState, parent, name, capacity);
  }

  @Override
  public void join() throws InterruptedException {
    agentThread.join();
  }

  @Override
  public void join(long millis) throws InterruptedException {
    agentThread.join(millis);
  }

  @Override
  protected void onStart() {
    agentThread.start();
  }

  @Override
  protected void onStop() {
    // Nothing to do
  }

  /**
   * This method implements the <code>Runnable</code> interface. It describes
   * the sequential code executed by message handling thread:
   * <ol>
   * <li>initialization,</li>
   * <li>message handling loop,</li>
   * <li>termination (caused by a stop event or an error).</li>
   * </ol>
   */
  @Override
  public void run() {
    while (!AgentStatus.STOPPED.equals(getStatus())) {
      try {
        consumeNextMessage();
      } catch (Exception e) {
        // Either thread has been interrupted, either an error occurred
        return;
      }
    }
  }

  /**
   * Sets the daemon flag of the thread that will execute message handling code.
   * This method must be called before the agent is started (i.e. before a call
   * to {@link #start()}).
   * 
   * @param on
   *          Value of the daemon flag.
   * 
   * @see Thread#setDaemon(boolean)
   */
  public void setDaemon(boolean on) {
    agentThread.setDaemon(on);
  }
}
