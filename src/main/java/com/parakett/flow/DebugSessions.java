package com.parakett.flow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class DebugSessions {

    private static HashMap<String, List<DebugStep>> _mDebugSteps = new HashMap<>();
    private static HashMap<String, Integer> _mDebugPointer = new HashMap<>();

    public static String newDebugSession() {
        String uid = UUID.randomUUID().toString();
        LinkedList<DebugStep> debugSteps = new LinkedList<>();
        _mDebugSteps.put(uid, debugSteps);

        return uid;
    }

    public void sessionFinished(String sessionId) {
        _mDebugPointer.put(sessionId, 0);
        _mDebugSteps.remove(sessionId);
    }

    public static void addStepRunInstance(String sessionId, StepRunInstance runInstance) {
        List<DebugStep> currentList = _mDebugSteps.get(sessionId);
        currentList.add(new DebugStep(runInstance, currentList.size()+1));
    }

    public static void run(String sessionId) {
        //Increase the sequence number and pick that.
        int sequence = _mDebugPointer.get(sessionId);
        sequence++;
         _mDebugPointer.put(sessionId, sequence);

    }

    public static class DebugStep {

        StepRunInstance runInstance = null;
        int stepSequence = 0;

        public DebugStep(StepRunInstance runInstance, int sequence) {
            this.runInstance = runInstance;
            this.stepSequence = sequence;
        }

    }
    
}
