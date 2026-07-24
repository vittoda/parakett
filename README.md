## Run the server.
### Setup
- **Build the required MCP servers**. Follow the corresponding instructions in [parakett_mcp](https://github.com/vittoda/parakett_mcp) repo for the instructions.
- **Configure MCP servers in Parakett server**. To use MCP servers, in Parakett server, you need to define them a config file. Format is simple. There are two config files provided out of the box.
	- `mcpServers.json`, which contains all the available MCP servers in mcp repo. Some of the servers defined in this file will need API keys. Check the documentation for the MCP server for more details
	- `mcpServersBasic.json`, which has subset of the MCP servers that will not need any API keys. 
	
	Update these configuration file as appropriate. In fact you can use your own file name. You need to define the config file using system property `fs.mcpConfigFile` when running the server. Otherwise it will default to `mcpServers.json` from the current folder.

- **Configure the agents**. Define your agents in **agents.json** or whatever file you prefer. Similar to the MCP servers, there are two agents configuration files available out of the box
	- `agents.json`, which contains agents that uses MCP servers which needed APIs keys (the ones in `mcpServers.json`).
	- `agentsBasic.json`, which contains agents with MCP servers that will not need API keys.

	Go through the agent configuration document, for more details
	
- **Update the ~/.fskeys** file with keys for **Gemini** or **github Model** API keys. The file is a simple *key=value* format.
	- For Gemini, use `parakett.gemini` as the key
	- For github Model API key use `parakett.github.openai` as the key
- **Update keys for channels**. Some of the channel implementation uses API key. Channel implementation are added as build dependencies. As of now slack is one one of the event channel. You can follow the instruction in respective documentation to get the keys, or remove the dependency from `build.gradle` file. If not you can always ignore the error, during server startup.

### Start the server
To start the server for development and testing, use the following command. Please note you will need JDK installed and and internet connection, as the command will pull additional dependencies

`./gradlew clean build bootRun`

You will need [Parakett UI](https://github.com/vittoda/parakett_ui) to channels to trigger any action. Check the documentation on how to run the UI instance. 

[Main Documentation](https://vittoda.github.io/parakett/)

