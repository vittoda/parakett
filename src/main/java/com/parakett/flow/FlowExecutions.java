package com.parakett.flow;

import java.util.HashMap;
import java.util.UUID;

import com.parakett.agent.Agent;

public class FlowExecutions {

    private static HashMap<String, FlowRunner> _mRunningFlows = new HashMap<>();

    public static FlowRunner getBySessionId(String sessionId) {
        return _mRunningFlows.get(sessionId);
    }

    public static FlowRunner getNewRunnerInstance(StepGroup steps, 
            boolean debug, boolean log, boolean archive,
            Agent agentInstance) {
        String sessionId = UUID.randomUUID().toString();
        FlowRunner runner = new FlowRunner(sessionId, steps,  debug, log, archive, agentInstance, agentInstance.getModelName());
        _mRunningFlows.put(sessionId, runner);
        FlowRunnerSessions.addFlow(runner);
        return runner;
    }

    public static void removeFlowRunner(String sessionId) {
        _mRunningFlows.remove(sessionId);
    }

    

}
