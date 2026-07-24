package com.parakett.flow;

import java.util.ArrayDeque;

public class FlowDebugState {

    FlowRunner flowRunner;

    private ArrayDeque<StepRunInstance> _mQueue = new ArrayDeque<>();

    private boolean _mAborted = false;

    public FlowDebugState(FlowRunner runner) {
        this.flowRunner = runner;
    }

    public void addNextStep(StepRunInstance step) {
        if (_mAborted) {
            return;
        }
        _mQueue.add(step);
    }

    public void run() {
        if (_mAborted) {
            return;
        }
        // Pick from queue and run
        StepRunInstance ri = _mQueue.poll();
        Thread thr = new Thread() {
            public void run() {
                ri.runNow();
            }
        };

        thr.start();
    }

    public boolean hasItems() {
        return !_mQueue.isEmpty();
    }

    public void setAborted(boolean aborted) {
        this._mAborted = aborted;
    }

    public boolean isAborted() {
        return this._mAborted;
    }

}
