package com.parakett.models;

public class GithubOpenAI extends OpenAI {

    private boolean _mLogRequests = false;

    public GithubOpenAI(String modelName) {
        super(modelName);
        _mLogRequests = System.getProperty("githubOpenAI.model.logRequests", "false").equals("true");
    }

    @Override
    protected String getCredKey() {
        return "parakett.github.openai";
    }

    @Override
    protected String getURL() {
        return "https://models.github.ai/inference/chat/completions";
    }

    @Override
    protected boolean logRequests() {
        return _mLogRequests;
    }

    

}
