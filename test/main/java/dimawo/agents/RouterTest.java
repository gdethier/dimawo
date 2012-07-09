package dimawo.agents;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RouterTest {

  private Router router;

  @Before
  public void before() {
    router = new Router();
  }

  @Test(expected = AgentException.class)
  public void duplicate() throws AgentException {
    String name = "name";
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn(name);
    router.registerAgent(agent);
    router.registerAgent(agent);
  }

  @Test
  public void register() throws AgentException {
    String name = "name";
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn(name);
    router.registerAgent(agent);
    AbstractAgent registered = router.getAgent(name);

    Assert.assertThat(registered, Is.is(agent));
  }

  @Test(expected = AgentException.class)
  public void registerEmpty() throws AgentException {
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn("");
    router.registerAgent(agent);
  }

  @Test(expected = AgentException.class)
  public void registerNull() throws AgentException {
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn(null);
    router.registerAgent(agent);
  }
}
