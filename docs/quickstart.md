# Quickstart

Here we will cover a quick usecase for Parakett—think of it as the 'Hello World' of your first program. For this guide, we won't be using any MCP servers or communication channels that require access tokens or complex credential setups. Before moving forward, ensure that you have followed the [installation](installation.md) instructions.

## Acces tokens for models.

You need to set the access token for the model. You will be using saving these access tokens in file `.fskeys` in your home directory. Following models are supported as of now

* *Github Models openai/gpt-4o*
* *OpenAI gpt-4o*
* *Gemini gemini-3.1-flash-lite*

*openai/gpt-4o* is the default model. You can create the access token for this using your github account. The free tier provides enough token allowance for this example. Once you obtain the token, create(or update) `.fskeys`, file in your home directory and add a key value pair, where your key is *parakett.github.openai* and value is the access token. For example

```
parakett.github.openai=github_pat_xxxxxxxxxxaaaaaa
```

If you have access keys for OpenAI or Gemini models, you can use them as well. Here are corresponding keys to be used `.fskeys` file.

* *OpenAI*, key : *parakett.openai*
* *Gemini*, key : *parakett.gemini*

But if you plan to use other models, you will need to update the `ModelConnectionRegistry.java` file to use the appropriate default model.

## Configure and Start Parakett Server

The Parakett server comes pre-configured with a robust set of defaults. Since we are running a simple introductory usecase, we will use an agent that executes local shell commands and coordinates with basic MCP servers. 

The core settings for this setup are defined in two configuration files:

- `mcpServersBasic.json` (MCP server configuration)
- `agentsBasic.json` (Agent definitions)

Run the following command to start the Parakett server:

```bash
cd parakett

java -Dfs.mcpConfigFile=mcpServersBasic.json \
     -Dfs.agentsConfigFile=agentsBasic.json \
     -Dmcp.base=<path_to_parakett_mcp_folder> \
     -jar build/libs/{{ parakett_server_binary }}
```

Make sure you replace *path_to_parakett_mcp_folder* with actual path to parakett_mcp folder. This will be relative path for MCP Server binaries defined in `mcpServersBasic.json`

Wait for few seconds for the Parakett server to initialize the MCP servers.

## Launch Parakett UI

We will use Parakett web UI to send some prompts to the *Shell* agent. Start the UI server using the following command

```
cd parakett_ui

npm run dev
```

Once the server is up, open the browser and enter the URL [http://localhost:5173/](http://localhost:5173/). 

On left navigation panel click on MCP Servers and Agents to see some if few models and ahents are listed.

## Use Web CLI interface 
We will use web CLI interface and get a local file created in `/tmp` folder with name `hello.txt` with content, *Hello World*. In real world, it will be faster to create the file directky. But this is just to test the setup.

1. Click the **Terminal** on the left navigation bar.
2. Connect to the server by clicking the **Connect** button. You will see the red dot changing to green and the log shows the version.
3. In the *Enter command* prompt, type/paste the following command. 

Make sure you enter the following in a single line
```
agent run shell --log --archive  Create a file /tmp/hello.txt,  with  single line text Hello World.
```

Press enter to run the CLI command. 

This command will send the prompt *Create a file /tmp/hello.txt,  with  single line text Hello World.* the agent *shell*. We will go through details of the syntax in another section. Once the all the steps are run, you will see an aknowledgement. Check the file to confirm if it is created.


Let us try another promot

```
agent run eventAgent --log --archive  I need to create a table 'fileinfo' in sqlite. Use demo1 as the database name. Table should have columns filename, filesize.
```

This is an example for a agent to agent communication. In the above example, *eventAgent* does not understand how to create a SQLite table. But it knows that agent *sqlite* can do it. So, it will generate a prompt and trigger *sqlite* agent. 

Note that SQlite MCP server will create the database `demo` in folder `/tmp` because, in `mcpServersBaisic.json`, we have specified dbPath system property *dbPath*.