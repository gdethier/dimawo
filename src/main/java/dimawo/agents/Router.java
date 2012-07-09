package dimawo.agents;

import java.util.HashMap;
import java.util.Map;

public class Router {

  private static Router defaultRouter = new Router();

  public static Router getDefaultRouter() {
    return defaultRouter;
  }

  @SuppressWarnings("rawtypes")
  private Map<String, AbstractAgent> registeredAgents = new HashMap<String, AbstractAgent>();

  @SuppressWarnings("unchecked")
  public synchronized <State> AbstractAgent<State> getAgent(String name) {
    return registeredAgents.get(name);
  }

  public synchronized <State> void registerAgent(AbstractAgent<State> agent)
      throws AgentException {
    String name = agent.getName();
    if (name == null || name.isEmpty()) {
      throw new AgentException("Cannot register an agent without name");
    }

    if (registeredAgents.get(name) != null) {
      throw new AgentException(
          "An agent with given name is already registered (" + name + ")");
    }

    registeredAgents.put(name, agent);
  }

  public synchronized void unregisterAgent(String name) throws AgentException {
    if (registeredAgents.get(name) == null) {
      throw new AgentException("No registered agent with name '" + name + "'");
    }

    registeredAgents.remove(name);
  }
}
