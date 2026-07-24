package com.parakett.agent;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Orchestrator {

     private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    public static Orchestrator INSTANCE = new Orchestrator();

    private HashMap<String, LinkedBlockingQueue<AgentMessage>> _mAgentQueues = new HashMap<>();

    private Orchestrator() {

    }

    public void registerAgent(String agentKey) {
        _mAgentQueues.put(agentKey, new LinkedBlockingQueue<AgentMessage>());
    }

    public void addMessage(String agentKey, AgentMessage am) {
        LOGGER.info("Adding message for agent '{}'", agentKey);
        _mAgentQueues.get(agentKey).add(am);
    }

    public AgentMessage poll(String agentKey) throws OrchestratorException {
        try {
            return _mAgentQueues.get(agentKey).take();
        } catch (InterruptedException e) {
            throw new OrchestratorException(e);
        }
    }

}
