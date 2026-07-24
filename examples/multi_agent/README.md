# Multi agent orchestration. 

**Multi-agent orchestration** is one of the core capability in Parakett, enabling one agent to trigger another based on the capabilities it exposes.

Rather than building a single monolithic agent with every feature built-in, you can design specialized micro-agents that coordinate with one another to achieve a goal. Note that "micro-agent" is purely a conceptual pattern. Parakett manages them as standard agents and doesn't need native awareness of the term to orchestrate them seamlessly.

In this example we will cover the following agents
- **Event orchestrator** : The agent will recieve events. It won't have any other knowledge. So, it will delegate some of the work to other agents.
- **SQLite**
- **Shell**

We will use a scenario where we need to get the OS version (using *Shell* agent), write to SQLite database.

## Instructions

1. Build Parakett Server, Parakett UI, Shell and SQLite agents.
2. Create a db in SQLite with name ma_example and table `os_versions`, using the following DDL.

    ```
    CREATE TABLE os_versions (
        os_version VARCHAR(100)
    );
    ```
3. Start Parakett Server.  Replace `mcp_server_folder` witho your MCP server repo root folder. 
    ```
    java \
        -Dfs.channelsConfigFile=./examples/multi_agent/channelsConfig.json \
        -Dmcp.base=<mcp_server_folder> \
	    -Dfs.mcpConfigFile=./examples/multi_agent/mcpServers.json \
	    -Dfs.agentsConfigFile=./examples/multi_agent/agents.json \
        -DopenAI.model.logRequests=true \
        -jar build/libs/parakett-0.0.1.jar
    ```
4. Start Parakett UI. Run the following command from parakett_ui repo folder
    ```
    npm run dev
    ```
5. Once the server starts, in web CLI, send the following prompt
    ```
    agent run eventAgent --log --archive You need to get the version number of the currently running OS of my machine. Add this to SQLite database 'ma_example', table 'os_versions'. You can use 'uname -r' to get the OS information
    ```

    Note : While LLM knows about `uname` command, we have explicitely mentioned in the prompt. This is because, during testing we have found that LLM generates multiple tool calls in the same step. This specific case was not handled in the orchestrator yet. (We will fix it wih high priority). Giving specific instruction generates only one tool call.

6. Check the table for the OS version.