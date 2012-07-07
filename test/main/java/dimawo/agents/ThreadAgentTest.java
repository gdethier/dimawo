package dimawo.agents;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ThreadAgentTest {

  private ThreadAgent<TestState> agent;

  @Before
  public void before() {
    agent = new ThreadAgent<TestState>(new TestState());
  }

  @Test
  public void consume() throws AgentException, InterruptedException {
    agent.registerHandler(Integer.class,
        new MessageHandler<TestState, Integer>() {
          @Override
          public void handle(TestState state, Integer message) throws Exception {
            state.state = message;
          }
        });

    agent.start();

    Integer newVal = 3;
    agent.submitMessage(newVal);

    agent.stop();
    agent.join();

    TestState agentState = agent.getState();
    Assert.assertThat(agentState.state, Is.is(3));
    Assert.assertThat(agent.getError(), IsNull.nullValue());
  }
}
