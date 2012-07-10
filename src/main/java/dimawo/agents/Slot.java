package dimawo.agents;

public class Slot {

  private final Object message;

  @SuppressWarnings("rawtypes")
  private final AbstractAgent agent;

  @SuppressWarnings("rawtypes")
  public Slot(Object message, AbstractAgent agent) {
    this.message = message;
    this.agent = agent;
  }

  @SuppressWarnings("rawtypes")
  public AbstractAgent getAgent() {
    return agent;
  }

  public Object getMessage() {
    return message;
  }

}
