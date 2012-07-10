package dimawo.agents;

import java.util.concurrent.LinkedBlockingQueue;

public class Worker implements Runnable {

  private final Scheduler scheduler;

  private final LinkedBlockingQueue<Slot> queue;

  private Thread thread;

  public Worker(Scheduler scheduler, LinkedBlockingQueue<Slot> queue) {
    this.scheduler = scheduler;
    this.queue = queue;
  }

  public void interrupt() {
    if (thread != null) {
      thread.interrupt();
    }
  }

  public void join() throws InterruptedException {
    if (thread != null) {
      throw new RuntimeException("Worker has not yet been started");
    }
    thread.join();
  }

  @Override
  public void run() {
    while (true) {
      try {
        Slot slot = queue.take();
        slot.getAgent().consumeMessage(slot.getMessage());
      } catch (Exception e) {
        scheduler.signalError(this, e);
        return;
      }
    }
  }

  public void start() {
    if (thread != null) {
      throw new RuntimeException("Worker has already been started");
    }

    thread = new Thread(null, this, "Worker");
    thread.start();
  }

}
