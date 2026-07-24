package com.parakett.flow;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FlowExecutionQueue {

    private final BlockingQueue<StepRunInstance> _mQueue = new LinkedBlockingQueue<>();

    public static final FlowExecutionQueue INSTANCE = new FlowExecutionQueue();

    private FlowExecutionQueue() {

    }

    public void startConsumer() {
        Thread consumer = new Thread(() -> {
            while (true) {
                try {
                    StepRunInstance step = _mQueue.take(); // blocks if empty
                    step.runNow();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // exit loop gracefully
                } 
            }
        });

        consumer.start();
    }

    public void addToQueue(StepRunInstance runInstance) {
        _mQueue.add(runInstance);
    }
    
}
