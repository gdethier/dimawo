package dimawo.agents;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import dimawo.agents.AbstractAgent.AgentStatus;
import dimawo.agents.events.InitAgent;
import dimawo.agents.events.StopAgent;

public class AbstractAgentTest {

  private AbstractAgent<TestState> agent;

  @Before
  public void before() {
    agent = new AbstractAgent<TestState>(new TestState()) {
      @Override
      public void join() throws InterruptedException {
      }

      @Override
      public void join(long millis) throws InterruptedException {
      }

      @Override
      protected void onStart() {
      }

      @Override
      protected void onStop() {
      }
    };
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

    Integer newVal = 3;
    agent.submitMessage(newVal);
    agent.consumeNextMessage();

    TestState agentState = agent.getState();
    Assert.assertThat(agentState.state, Is.is(3));
    Assert.assertThat(agent.getError(), IsNull.nullValue());
  }

  @Test
  public void consumeError() throws AgentException, InterruptedException {
    agent.submitMessage(new Exception("error"));
    agent.consumeNextMessage();
    Assert.assertThat(agent.getError(), IsNull.notNullValue());
    Assert.assertThat(agent.getStatus(), Is.is(AgentStatus.STOPPED));
  }

  @Test
  public void flow() throws Exception {
    Assert.assertThat(agent.getStatus(), Is.is(AbstractAgent.AgentStatus.INIT));

    MessageHandler<TestState, InitAgent> initHandler = Mockito
        .mock(MessageHandler.class);
    MessageHandler<TestState, StopAgent> exitHandler = Mockito
        .mock(MessageHandler.class);

    agent.registerInitHandler(initHandler);
    agent.registerExitHandler(exitHandler);

    agent.start();
    agent.consumeNextMessage(); // consume init event
    Assert.assertThat(agent.getStatus(),
        Is.is(AbstractAgent.AgentStatus.RUNNING));
    Mockito.verify(initHandler).handle(Mockito.any(TestState.class),
        Mockito.any(InitAgent.class));

    agent.stop();
    agent.consumeNextMessage(); // consume exit event
    Assert.assertThat(agent.getStatus(),
        Is.is(AbstractAgent.AgentStatus.STOPPED));
    Mockito.verify(exitHandler).handle(Mockito.any(TestState.class),
        Mockito.any(StopAgent.class));
  }
}
