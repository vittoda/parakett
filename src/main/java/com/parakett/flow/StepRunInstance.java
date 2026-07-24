package com.parakett.flow;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.parakett.agent.AgentMessage;
import com.parakett.agent.Orchestrator;
import com.parakett.mcp.MCPRegistry;
import com.parakett.models.ModelConnection;
import com.parakett.models.ModelConnectionRegistry;
import com.parakett.models.ModelException;
import com.parakett.models.ModelNonRecoverableException;
import com.parakett.models.ModelResponse;
import com.parakett.models.ToolMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepRunInstance {

    private static final Logger LOGGER = LoggerFactory.getLogger(StepRunInstance.class);

    public static final String STATUS_WAITING = "Waiting";
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_HUMAN_IN_LOOP = "HIL";
    public static final String STATUS_SUCCESS = "Success";
    public static final String STATUS_FAILED = "Failed";

    public static final String FAIL_REASON_REQUEST_FAILED = "Request Failed";
    public static final String FAIL_REASON_INVALID_RESPONSE = "Invalid Response";
    public static final String FAIL_REASON_INVALID_STEP = "Invalid step";
    public static final String FAIL_REASON_NON_RECOVERABLE_ERROR = "Non recoverable error";

    private FlowRunner _mFlowRunner = null;
    private int _mStepSequence = -1;
    private Step _mStepDefinition = null;
    private String _mStatus = STATUS_WAITING;
    private String _mErrorMessage = null;
    private Exception _mException = null;
    private ModelResponse _mResponse = null;
    private String _mAdditionalInstructions = null;
    private boolean _mNeedHIL = false;

    private FlowRunLog.FlowRunLogItem _mRunLogItem = null; // Log info, but specific to execution. Not generic.

    // Execution related.
    private String _mModelName = null;
    private List<ToolMessage> _mTools = null;

    private List<String> _mUserMessages = null;
    private List<String> _mAdditionalSystemMessage = null;

    public StepRunInstance(FlowRunner flowRunner, int stepSequence,
            Step stepDefinition,
            String additionalInstructions,
            boolean needHil) {
        this._mFlowRunner = flowRunner;
        this._mStepSequence = stepSequence;
        this._mStepDefinition = stepDefinition;
        this._mAdditionalInstructions = additionalInstructions;
        this._mNeedHIL = needHil;
    }

    public Step getStepDefinition() {
        return _mStepDefinition;
    }

    public int getSequenceNumber() {
        return _mStepSequence;
    }

    public String getStatus() {
        return _mStatus;
    }

    public void setStatus(String status) {
        this._mStatus = status;
    }

    public String getErrorMessage() {
        return _mErrorMessage;
    }

    public Exception getException() {
        return _mException;
    }

    public void addLogInfo(String key, String v) {
        if (_mRunLogItem != null) {
            _mRunLogItem.setAdditionalLogInfo(key, v);
        }
    }

    public void addLogInfo(String key, ArrayNode v) {
        if (_mRunLogItem != null) {
            _mRunLogItem.setAdditionalLogInfo(key, v);
        }
    }

    public void addLogInfo(String key, Long v) {
        if (_mRunLogItem != null) {
            _mRunLogItem.setAdditionalLogInfo(key, v);
        }
    }

    public void addToDebugLog() {
        _mRunLogItem = _mFlowRunner.addLog(_mStepDefinition);
    }

    public void prepare() {
        switch (_mStepDefinition.type) {
            case Step.STEP_TYPE_LLM:
                prepareLLMStep();
                break;
            case Step.STEP_TYPE_AGENT:
                prepareAgentStep();
                break;
            default:
                LOGGER.error("Unknown step type '{}'", _mStepDefinition.type);
                if (_mRunLogItem != null) {
                    _mRunLogItem.update(STATUS_FAILED, FAIL_REASON_INVALID_STEP);
                }
                _mFlowRunner.currentStepCompleted(false);
        }
    }

    private void prepareAgentStep() {

    }

    private String replaceVariables(Variables variables, String inputText) {
        for (String name : variables.keys()) {
            inputText = inputText.replace("{{" + name + "}}", variables.getValue(name) + "");
        }

        return inputText;
    }

    private void prepareLLMStep() {

        _mUserMessages = new LinkedList<>();
        String instructions = _mStepDefinition.instruction;
        if (_mAdditionalInstructions != null && _mAdditionalInstructions.length() > 0) {
            _mUserMessages.add(_mAdditionalInstructions);
        }

        instructions = replaceVariables(_mFlowRunner.variables, instructions);
        if (_mRunLogItem != null) {
            _mRunLogItem.setInstructions(instructions);
        }

        if (_mStepDefinition.jsonResponse) {
            if (_mAdditionalSystemMessage == null) {
                _mAdditionalSystemMessage = new LinkedList<>();
            }
            _mAdditionalSystemMessage.add("You will provide the response ony in JSON format");
        }

        // Do we have user content ?
        if (_mStepDefinition.instruction != null && _mStepDefinition.instruction.length() > 0) {
            String ic = replaceVariables(_mFlowRunner.variables, _mStepDefinition.instruction);
            _mUserMessages.add(ic);
        }

        String[] toolNames = _mStepDefinition.toolNames;
        if (toolNames != null && toolNames.length > 0) {
            _mTools = new LinkedList<>();
            for (String toolName : toolNames) {
                if (toolName.startsWith("agent_")) {
                    _mTools.add(new ToolMessage(AgentTool.getDefinitionForFunctionCalling(toolName)));
                } else {
                    _mTools.add(new ToolMessage(MCPRegistry.getToolDefinitionForToolCalling(toolName)));
                }
            }
        }

        // Get the model.
        _mModelName = _mStepDefinition.model;
        if (_mModelName == null) {
            // Get the flow model .
            // TODO: Ideally, you should go on the hierarchy and get the model for the
            // parent till root. But note that, we cannot just traverse back from current
            // step, because
            // there are more than one parent step for a specific step. So, instead when
            // executing, based on the parent ste
            // when child node run instance is created.
            _mModelName = _mFlowRunner.getModelName();
        }

        if (_mRunLogItem != null) {
            _mRunLogItem.setMemory(_mFlowRunner.getMemory().getMergedSnapshot());
        }

        if (_mRunLogItem != null) {
            _mRunLogItem.setVariables(_mFlowRunner.variables);
        }
    }

    public void runNow() {
        // Check if this step needs
        LOGGER.info("Running step '{}'", _mStepDefinition.name);
        if (this._mNeedHIL) {
            try {
                _mStatus = STATUS_HUMAN_IN_LOOP;
                if (_mRunLogItem != null) {
                    _mRunLogItem.update(_mStatus, null);
                }
                _mFlowRunner.runHilWebHook(this);

            } catch (FlowException e) {
                e.printStackTrace();
                _mStatus = STATUS_FAILED;
                if (_mRunLogItem != null) {
                    _mRunLogItem.update(_mStatus, e.getMessage());
                }
                _mFlowRunner.setStatus((FlowRunner.STATUS_FAILED));
            }
            return;
        }

        _resumeExecution();
    }

    void _resumeExecution() {
        if (_mRunLogItem != null) {
            _mRunLogItem.setAdditionalLogInfo("stepStartTime", new java.util.Date().getTime());
        }
        _mStatus = STATUS_RUNNING;
        if (_mRunLogItem != null) {
            _mRunLogItem.update(_mStatus, null);
        }

        try {
            switch (_mStepDefinition.type) {
                case Step.STEP_TYPE_LLM:
                    runLLMNow();
                    break;
                case Step.STEP_TYPE_AGENT:
                    runAgentNow();
                    break;
            }

            if (_mRunLogItem != null) {
                _mRunLogItem.setAdditionalLogInfo("stepEndTime", new java.util.Date().getTime());
            }
        } catch (FlowNonRecoverableException e) {
            if (_mRunLogItem != null) {
                _mRunLogItem.setAdditionalLogInfo("stepEndTime", new java.util.Date().getTime());
            }
            e.printStackTrace();
            _mFlowRunner.currentStepFailed(StepRunInstance.FAIL_REASON_NON_RECOVERABLE_ERROR, e.getMessage());
        } catch (FlowException e) {
            if (_mRunLogItem != null) {
                _mRunLogItem.setAdditionalLogInfo("stepEndTime", new java.util.Date().getTime());
            }
            e.printStackTrace();
            _mFlowRunner.currentStepFailed(StepRunInstance.FAIL_REASON_INVALID_RESPONSE, e.getMessage());
        }
    }

    private void runAgentNow() throws FlowException {
        // Here we are sending the instruction new
        String agentId = _mStepDefinition.agent;

        List<String> additionalContext = _mFlowRunner.getAdditionalContextsForStep(_mStepDefinition.name);
        LOGGER.info("Adding message for agent in step '{}'. Additional context is NULL ? {}", _mStepDefinition.name, (additionalContext == null));
        AgentMessage am = new AgentMessage(_mStepDefinition.instruction, additionalContext, this._mFlowRunner);
        Orchestrator.INSTANCE.addMessage(agentId, am);

        // We need mark the flow to hold.
        _mFlowRunner.waitForAgentResponse(this);

    }

    private void runLLMNow() throws FlowException {
        ModelConnection mc = ModelConnectionRegistry.getConnectionForModel(_mModelName);
        long startTime = System.currentTimeMillis();

        try {

            ModelResponse resp = mc.sendRequest(_mFlowRunner.getMemory(), _mAdditionalSystemMessage,
                    _mUserMessages,
                    _mTools, _mStepDefinition.jsonResponse);
            long endTime = System.currentTimeMillis();
            if (_mRunLogItem != null) {
                _mRunLogItem.setAdditionalLogInfo("modelRequestDuration", (endTime - startTime));
            }
            _mResponse = resp;
            if (_mRunLogItem != null && resp != null) {
                _mRunLogItem.setLLMResponse(resp.assistantMessage);
            }

        } catch (ModelNonRecoverableException e) {
            e.printStackTrace();
            throw new FlowNonRecoverableException(e.getMessage());
        } catch (ModelException e) {
            long endTime = System.currentTimeMillis();
            if (_mRunLogItem != null) {
                _mRunLogItem.setAdditionalLogInfo("modelRequestDuration", (endTime - startTime));
            }

            if (_mRunLogItem != null) {
                _mRunLogItem.update(STATUS_FAILED, FAIL_REASON_INVALID_RESPONSE);
                _mRunLogItem.setErrorMessage(e.getMessage());
            }
            _mException = e;
            _mErrorMessage = e.getMessage();
            throw new FlowException(e);
        }

        try {
            ModelResponseHandler responseHandler = new ModelResponseHandler();
            responseHandler.process(this, _mFlowRunner, _mResponse);
            if (_mRunLogItem != null) {
                _mRunLogItem.update(STATUS_SUCCESS, null);
            }
        } catch (FlowException e) {
            if (_mRunLogItem != null) {
                _mRunLogItem.update(STATUS_FAILED, FAIL_REASON_INVALID_RESPONSE);
                _mRunLogItem.setErrorMessage(e.getMessage());
            }
            _mException = e;
            _mErrorMessage = e.getMessage();
            throw e;
        }
    }

}
