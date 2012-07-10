package dimawo.agents;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SchedulerTest {

  private static int THREADS = 5;

  private Scheduler scheduler;

  @Before
  public void before() {
    scheduler = new Scheduler(THREADS);
  }

  @Test
  public void testLoadBalancing() throws InterruptedException {
    int factor = 3;

    for (int i = 0; i < factor * THREADS; ++i) {
      scheduler.submitMessage(null, null);
    }

    int[] levels = scheduler.getQueueLevels();
    for (int l : levels) {
      Assert.assertThat(new Integer(l), Is.is(new Integer(factor)));
    }
  }
}
