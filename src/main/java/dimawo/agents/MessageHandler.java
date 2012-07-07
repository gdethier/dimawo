package dimawo.agents;

public interface MessageHandler<State, Message> {
    void handle(State agentState, Message message) throws Exception;
}
