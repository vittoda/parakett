package com.parakett.flow;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.agent.Agent;
import com.parakett.agent.HITLConfig;
import com.parakett.channels.base.CommChannelException;
import com.parakett.channels.base.CommChannelInstance;
import com.parakett.channels.base.MessageContext;
import com.parakett.channels.base.OutputMessage;
import com.parakett.agent.HITLConfig.HITLTarget;
import com.parakett.models.ModelSystemMessage;
import com.parakett.models.ModelUserMessage;
import com.parakett.models.ToolResponseMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowRunner.class);

    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_ABORTED = "Aborted";
    public static final String STATUS_READY = "Ready";
    public static final String STATUS_HUMAN_IN_LOOP = "HIL";
    public static final String STATUS_HUMAN_IN_LOOP_FAILED = "HIL Failed";
    public static final String STATUS_WAIT_FOR_AGENT_RESULT = "Hold";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_SUCCESS = "Success";

    private static final int MAX_ERROR_RETRIES = 2;

    public enum UnholdMode {
        FAILED,
        NORMAL
    }

    private static HttpClient _mClient = null;

    private String _mModelName = null;

    private boolean _mArchiveSession = true; // If true, when the session is completed, it is not removed.
    private boolean _mIsDebugMode = false;
    private boolean _mLogModeEnabled = false;
    private FlowRunLog _mLog = null;
    private FlowDebugState _mDebugState = null;
    private Date _mStartTime = null;

    private String _mStatus = STATUS_READY;
    private String _mSessionId = null;
    private String _mCurrenStepName = null;
    private StepGroup _mStepGroup = null;
    private int _mNextSequenceNumber = 0;

    private StepRunInstance _mHoldingRunInstance = null;

    private FlowMemory _mMemory = new FlowMemory();
    private Agent _mAgent = null;

    private String _mFlowSessionToNotify = null;
    private MessageContext _mTriggerringMessageContext = null;
    private CommChannelInstance _mTriggeringChannel = null;

    private int _mErrorRetryCount = 0;

    Variables variables = new Variables();

    private String _mFlowResult = null;
    private HashMap<String, List<String>> _mStepAdditonalContext = new HashMap<>();

    FlowRunner(String sessionId, StepGroup steps,
            boolean debugMode, boolean logModeEnabled, boolean archiveEnabled,
            Agent agent, String modelName) {
        _mSessionId = sessionId;
        this._mAgent = agent;
        this._mIsDebugMode = debugMode;
        if (_mDebugState == null) {
            _mDebugState = new FlowDebugState(this);
        }
        _mLogModeEnabled = logModeEnabled;
        if (this._mLogModeEnabled) {
            _mLog = new FlowRunLog();
        }

        _mStartTime = new Date();
        this._mArchiveSession = archiveEnabled;
        _mStepGroup = steps;
        _mModelName = modelName;
    }

    public String getModelName() {
        return _mModelName;
    }

    public String getSessionId() {
        return _mSessionId;
    }

    public boolean isLoggingEnabled() {
        return _mLogModeEnabled;
    }

    public boolean isDebugEnabled() {
        return _mIsDebugMode;
    }

    public boolean isArchiveEnabled() {
        return _mArchiveSession;
    }

    public ObjectNode getJSON(boolean extended) {
        ObjectNode ret = JsonUtils.MAPPER.createObjectNode();
        ret.put("sessionId", _mSessionId);
        ret.put("debug", _mIsDebugMode);
        ret.put("debugAborted", _mDebugState.isAborted());
        ret.put("startTime", _mStartTime.getTime());
        ret.put("status", _mStatus);

        return ret;
    }

    public void setFlowToNotify(String sessionId) {
        _mFlowSessionToNotify = sessionId;
    }

    public void setTriggerringMessageContext(MessageContext ctx) {
        _mTriggerringMessageContext = ctx;
    }

    public MessageContext getTriggerringMessageContext() {
        return _mTriggerringMessageContext;
    }

    public void setTriggerringChannel(CommChannelInstance channelInstance) {
        _mTriggeringChannel = channelInstance;
    }

    public CommChannelInstance getTriggerringChannel() {
        return _mTriggeringChannel;
    }

    public void setFlowResult(String result) {
        _mFlowResult = result;
    }

    public ObjectNode getResult() {
        ObjectNode res = JsonUtils.MAPPER.createObjectNode();
        res.put("status", _mStatus);
        res.put("result", _mFlowResult);

        return res;
    }

    public void setAdditionalContextForStep(String stepName, String context)  {
        List<String> contexts = _mStepAdditonalContext.get(stepName);
        if(contexts == null) {
            contexts = new LinkedList<>();
            _mStepAdditonalContext.put(stepName, contexts);
        }

        contexts.add(context);
    }

    public List<String> getAdditionalContextsForStep(String stepName) {
        return _mStepAdditonalContext.get(stepName);
    }

    public Agent getAgentInstance() {
        return _mAgent;
    }

    public void setVariable(String key, Object value) {
        variables.setValue(key, value);
    }

    public String getStatus() {
        return _mStatus;
    }

    void setStatus(String status) {
        _mStatus = status;
    }

    public FlowRunLog getRunLog() {
        return _mLog;
    }

    public boolean isLogEnabled() {
        return this._mLogModeEnabled;
    }

    public ObjectNode sendResponse(String message) throws FlowException {
        try {
            OutputMessage om = new OutputMessage(message, _mTriggerringMessageContext);
            _mTriggeringChannel.sendMessage(om);
            ObjectNode resp = JsonUtils.MAPPER.createObjectNode();
            resp.put("status", "success");
            resp.put("message", "Message sent");
            return resp;
        } catch (CommChannelException e) {
            throw new FlowException(e);
        }
    }

    private void completed() {
        // Check if we have session to notify before
        LOGGER.info("Flow completed. To notify '{}'", _mFlowSessionToNotify);
        if (!_mStatus.equals(STATUS_FAILED)) {
            _mStatus = STATUS_SUCCESS;
        }
        if (_mFlowSessionToNotify != null) {
            FlowExecutions.getBySessionId(_mFlowSessionToNotify)
                    .unholdExecution(_mStatus.equals(STATUS_FAILED) ? UnholdMode.FAILED : UnholdMode.NORMAL,
                            getResult());
        } else {
            // Notify that flow session is completed.
            if (_mTriggerringMessageContext != null) {
                try {
                    if (_mStatus.equals(STATUS_FAILED)) {
                        sendResponse("Session '" + _mSessionId + "' failed.");
                    } else {
                        sendResponse("Session '" + _mSessionId + "' completed.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void abortDebug() {
        _mDebugState.setAborted(true);
    }

    public void unholdExecution(UnholdMode unHoldMode, ObjectNode result) {
        LOGGER.info("Unhold the flow. '{}'", _mSessionId);
        if (_mStatus.equals(STATUS_WAIT_FOR_AGENT_RESULT)) {

            StringBuilder resultBuilder = new StringBuilder("Result from step '" + _mCurrenStepName + "'.\n");
            resultBuilder.append("Status : " + result.get("status").asText()).append("\n");
            resultBuilder.append("Result : " + result.get("result").asText());
            if (unHoldMode == UnholdMode.FAILED
                    || _mHoldingRunInstance.getStatus().equals(StepRunInstance.STATUS_FAILED)) {
                _mStatus = STATUS_FAILED;
                currentStepFailed("Agent call failed", "");
            } else {
                _mStatus = STATUS_RUNNING;
                currentStepCompleted(true);
            }
            // Add to the flow result.
            _mHoldingRunInstance = null;

            ModelSystemMessage ms = new ModelSystemMessage(resultBuilder.toString());
            _mMemory.addContent(ms);
            return;
        }
        else if(_mStatus.equals(STATUS_HUMAN_IN_LOOP)) {
            List<String> hilResultList = new LinkedList<>();
            String userMessage = "Human in loop, user response message : \n\n" +result.toString();
            hilResultList.add(userMessage);
            ModelUserMessage mu = new ModelUserMessage(hilResultList);
            _mMemory.addContent(mu);
        }

        _mHoldingRunInstance._resumeExecution();
        _mHoldingRunInstance = null;
        _mStatus = STATUS_RUNNING;
    }

    public Step getCurrentStep() {
        return _mStepGroup.getStep(_mCurrenStepName);
    }

    /*
     * Abort the execution. Current step will be taken for completion.
     */
    // TODO: Make an attempt to stop the current step.
    public void abort() {
        _mStatus = STATUS_ABORTED;
        if (_mDebugState != null) {
            _mDebugState.setAborted(true);
        }
        breakCurrentFlow();
    }

    private void breakCurrentFlow() {
        _mHoldingRunInstance = null;
        _mCurrenStepName = null;
        FlowExecutions.removeFlowRunner(_mSessionId);
        if (!this._mArchiveSession) {
            FlowRunnerSessions.removeFlowRunner(_mSessionId);
        }
        completed();
    }

    public void runStep(String name) {
        runStep(name, null);
    }

    public void runCurrentStepAgain() {
        runStep(_mCurrenStepName, null);
    }

    public void runStep(String name, String additionalInstructions) {
        if (name.equals("__error__")) {
            _mErrorRetryCount++;
            if (_mErrorRetryCount > MAX_ERROR_RETRIES) {
                // Fail the step.
                _mStatus = STATUS_FAILED;
                if (_mTriggerringMessageContext != null) {
                    try {
                        sendResponse("Session '" + _mSessionId
                                + "' failed. Error message : Maximum error recovery attempt reached");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                breakCurrentFlow();
                return;
            }
        } else {
            _mErrorRetryCount = 0;
        }
        Step stepToRun = _mStepGroup.getStep(name);
        _mCurrenStepName = stepToRun.name;
        _mStatus = STATUS_RUNNING;

        StepRunInstance runInstance = new StepRunInstance(this, _mNextSequenceNumber, stepToRun,
                additionalInstructions, stepToRun.hitlNeeded);
        _mNextSequenceNumber++;

        if (this._mLogModeEnabled) {
            runInstance.addToDebugLog();
        }
        LOGGER.info("Ppeparing the steps.");
        runInstance.prepare();
        if (this._mIsDebugMode) {
            _mDebugState.addNextStep(runInstance);
        } else {
            FlowExecutionQueue.INSTANCE.addToQueue(runInstance);
        }
    }

    /*
     * Remove the current step group and start new. This is used when we get the
     * steps list from the LLM and remove the root,
     * to start new.
     */
    void clearAndRunStepGroup(StepGroup group, Step stepToRun, String promptMessage) {
        _mNextSequenceNumber = 0;
        _mStepGroup = group;
        
        StepRunInstance runInstance = new StepRunInstance(this, _mNextSequenceNumber, stepToRun, null,
                stepToRun.hitlNeeded);
        _mCurrenStepName = stepToRun.name;
        _mNextSequenceNumber++;
        if (this._mLogModeEnabled) {
            runInstance.addToDebugLog();
        }

        // Clear the memory.
        _mMemory.clear();
        
        List<String> systemInstructions = _mAgent.getInitialStepSystemInstructionsForDomain();
        for (String s : systemInstructions) {
            _mMemory.addContent(new ModelSystemMessage(s));
        }
        if( promptMessage != null && promptMessage.length() > 0) {
            LinkedList<String> userMessage = new LinkedList<>();
            userMessage.add(promptMessage);
            _mMemory.addContent(new ModelUserMessage(userMessage));
        }
        runInstance.prepare();
        if (_mIsDebugMode) {
            _mDebugState.addNextStep(runInstance);
        } else {
            FlowExecutionQueue.INSTANCE.addToQueue(runInstance);
        }
    }

    public void debugNextStep() {
        _mDebugState.run();
    }

    public void currentStepFailed(String failReason, String errorMessage) {
        if (_mLogModeEnabled) {
            FlowRunLog.FlowRunLogItem item = _mLog.getLastItem();
            item.update(StepRunInstance.STATUS_FAILED, failReason);
            item.setErrorMessage(errorMessage);
        }
        _mStatus = STATUS_FAILED;

        // Find the error step and run. But if it is unrecoverable error, we need to
        // exit now.
        if (!failReason.equals(StepRunInstance.FAIL_REASON_NON_RECOVERABLE_ERROR)) {
            LOGGER.warn("Flow failed with non-recoverable error. Message : {}" , errorMessage);
            runErrorStep();
        } else {
            // Notify if anyone is listenening.
            if (_mTriggerringMessageContext != null) {
                try {
                    sendResponse("Session '" + _mSessionId + "' failed. Error message : " + errorMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            currentStepCompleted(false);
        }

    }

    public void runErrorStep() {
        runStep("__error__", null);
    }

    /*
     * This should be called only when model did not mention which step to run (for
     * example cases of tool calling)
     */
    public void currentStepCompleted(boolean startNextStep) {
        Step currentStep = _mStepGroup.getStep(_mCurrenStepName);
        if (startNextStep == false) {
            if (currentStep.nextSteps == null || currentStep.nextSteps.length == 0) {
                completed();
            }
            return;
        }

        // Do we have next step ?
        if (currentStep.nextSteps != null && currentStep.nextSteps.length > 0) {
            String nextStepName = currentStep.nextSteps[0];
            runStep(nextStepName, null);
        } else {
            completed();
        }
    }

    void waitForAgentResponse(StepRunInstance stepRunInstance) throws FlowException {
        _mStatus = STATUS_WAIT_FOR_AGENT_RESULT;
        _mHoldingRunInstance = stepRunInstance;
    }

    void runHilWebHook(StepRunInstance stepRunInstance) throws FlowException {
        _mStatus = STATUS_RUNNING;

        // Get the HITL config
        HITLMessage hitlConfig = stepRunInstance.getStepDefinition().hitlMessage;

        HITLTarget hitlTargets[] = _mAgent.getHITLConfig().targets;

        for (HITLTarget t : hitlTargets) {
            try {
                if (t.type.equals(HITLConfig.HITL_TARGET_TYPE_EVENT)) {
                    // Only event channel is supported as of now.
                    getReponseFromEventChannel(hitlConfig.message, hitlConfig.responseKey, this._mSessionId);
                } else if (t.type.equals(HITLConfig.HITL_TARGET_TYPE_WEBHOOK)) {
                    getResponseFromWebHook(t.instance, hitlConfig.message, hitlConfig.responseKey, this._mSessionId);
                }
            } catch (Exception e) {
                //Ignore, as one might fail and other might siceed.
                //TODO: Handle if all failed.
                LOGGER.error("Error in submitting HITL request",e);
            }
        }

        _mHoldingRunInstance = stepRunInstance;
        _mStatus = STATUS_HUMAN_IN_LOOP;

    }

    private void getReponseFromEventChannel(String message, String responseKey, String requestId) {
        if (_mTriggeringChannel != null) {
            try {
                _mTriggeringChannel.getConfirmationResponse(_mTriggerringMessageContext,
                        message, responseKey, requestId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getResponseFromWebHook(String url, String message, String responseKey, String requstId)
            throws FlowException {
        if (_mClient == null) {
            _mClient = HttpClient.newHttpClient();
        }

        try {

            ObjectNode requestObject = JsonUtils.MAPPER.createObjectNode();
            requestObject.put("sessionId", _mSessionId);
            requestObject.put("requstId", requstId);
            requestObject.put("message", message);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestObject.toString()))
                    .build();

            HttpResponse<String> response = _mClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new FlowException("Webhook responded with status code '" + response.statusCode() + "'");
            }

        } catch (IOException | InterruptedException e) {
            throw new FlowException(e);
        }
    }

    void addToolResult(String toolName, String toolId, ObjectNode result) {
        // Add the item to memory.
        ToolResponseMessage tm = new ToolResponseMessage(toolId, toolName, result.toString());
        _mMemory.addContent(tm);
        // Add to the log result as well
        if (_mLogModeEnabled) {
            _mLog.getLastItem().setToolResult(result);
        }
    }

    public FlowMemory getMemory() {
        return _mMemory;
    }

    FlowRunLog.FlowRunLogItem addLog(Step step) {
        if (this._mLogModeEnabled) {
            return _mLog.add(step);
        }

        return null;
    }
}
