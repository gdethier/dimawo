package dimawo.agents;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PoolAgent<State> extends AbstractAgent<State> {

  private Scheduler scheduler = Scheduler.getDefaultScheduler();

  private Semaphore joinSync = new Semaphore(0);

  public PoolAgent(State initialState) {
    super(initialState);
  }

  public PoolAgent(State initialState, ErrorHandler parent, String name) {
    super(initialState, parent, name);
  }

  @Override
  public void join() throws InterruptedException {
    joinSync.acquire();
    joinSync.release();
  }

  @Override
  public void join(long millis) throws InterruptedException {
    if (joinSync.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
      joinSync.release();
    }
  }

  @Override
  protected void onStart() {
    // Nothing to do
  }

  @Override
  protected void onStop() {
    joinSync.release();
  }

  public void setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void submitMessage(Object o) throws InterruptedException {
    scheduler.submitMessage(this, o);
  }

}
