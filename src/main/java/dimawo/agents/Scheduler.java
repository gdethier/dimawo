package dimawo.agents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Scheduler {

  private static Scheduler defaultScheduler = new Scheduler(5);

  public static Scheduler getDefaultScheduler() {
    return defaultScheduler;
  }

  private List<Worker> workers = new ArrayList<Worker>();

  private List<LinkedBlockingQueue<Slot>> queues = new ArrayList<LinkedBlockingQueue<Slot>>();

  private Iterator<LinkedBlockingQueue<Slot>> queueIterator;

  private List<Exception> errors = new ArrayList<Exception>();

  public Scheduler(int numOfThreads) {
    if (numOfThreads <= 0) {
      throw new RuntimeException("Number of threads must be greater than zero");
    }

    for (int i = 0; i < numOfThreads; ++i) {
      addNewWorker();
    }

    queueIterator = queues.iterator();
  }

  private void addNewWorker() {
    LinkedBlockingQueue<Slot> queue = new LinkedBlockingQueue<Slot>();
    Worker worker = new Worker(this, queue);
    workers.add(worker);
    queues.add(queue);
  }

  public synchronized int[] getQueueLevels() {
    int[] levels = new int[queues.size()];
    int i = 0;
    for (LinkedBlockingQueue<Slot> q : queues) {
      levels[i++] = q.size();
    }
    return levels;
  }

  public void join() throws InterruptedException {
    queueIterator = queues.iterator();
    for (Worker w : workers) {
      w.join();
    }
  }

  synchronized void signalError(Worker worker, Exception e) {
    queueIterator = null;
    errors.add(e);
    for (Worker w : workers) {
      w.interrupt();
    }
  }

  public synchronized void start() {
    for (Worker w : workers) {
      w.start();
    }
  }

  @SuppressWarnings("rawtypes")
  void submitMessage(AbstractAgent agent, Object message)
      throws InterruptedException {

    LinkedBlockingQueue<Slot> queue = null;
    synchronized (this) {
      if (queueIterator == null) {
        throw new RuntimeException("Scheduler is not running");
      }

      if (!queueIterator.hasNext()) {
        queueIterator = queues.iterator();
      }

      queue = queueIterator.next();
    }

    queue.put(new Slot(message, agent));
  }

}
