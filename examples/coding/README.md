# Coding Agent
This example demonstrates how to build a basic code-runner agent using the framework. It isn't intended to compare against full-featured tools in thi domain, but rather to showcase how Parakett can be applied across different domain use cases.

In this example we will be generating a simple multi file java program, compile build and run. We created a deducared agent for the same which uses *codeRun* and *shell* MCP servers.

## Instructions
1. As a pre-req, build Parakett server, Parakett UI, in addition to codeRun and sheel MCP servers. 
2. Create a folder `/tmp/ecom`, where the code will be generated. If you prefer some other folder, ensure you change the same in input prompt.
3. We will use same java bananries that we used for building and running Parakett servers.
4. Run the Parakett server. Replace (replace `mcp_server_folder` with MCP servers root folder)
    ```
    java -Dfs.mcpConfigFile=examples/coding/mcpServers.json \
       -Dfs.agentsConfigFile=examples/coding/agents.json \
       -Dfs.channelsConfigFile=examples/coding/channelsConfig.json \
       -Dmcp.base=<mcp_server_folder> \
        -DopenAI.model.logRequests=true \
       -jar build/libs/parakett-0.0.1.jar
    ```
5. Start the Parakett web ui. Run the command from web ui repo root folder.
    ```
    npm run dev
    ```
6. Once the servers are up and running enter the following promt in the Web CLI
    ```
    agent run coding --log --archive Create and execute a Java application that calculates the total cost of an e-commerce order using dynamic discount strategies. ### Architecture Requirements Organize the solution into a domain model split across 5 separate class files in the current working directory: 1. A domain class representing a line item with properties for product ID, name, unit price, and quantity, along with a method to calculate the line item subtotal. 2. An interface defining a contract for applying a discount strategy to a total amount. 3. An implementation of the discount interface that applies a percentage discount (e.g., 10%) if the order subtotal exceeds $100. 4. An order management class that maintains a list of line items and an attached discount strategy. It must include methods to compute subtotal, discount amount, and final total. 5. An executable entry-point class containing the main method. It must: - Instantiate 3–4 sample line items (e.g., Laptop @ $850.00 x 1, Mouse @ $25.00 x 2, Keyboard @ $75.00 x 1). - Construct an order, assign the percentage discount strategy, and execute the calculation. - Print only the clean, final summary to standard output. ### Output Constraints - CRITICAL: Do not output compile logs, step-by-step progress, or debug logs. - Only print the final structured output to standard output upon running the program. ### Expected Output Format --- Order Summary --- Subtotal: $975.00 Discount Applied (10%): -$97.50 Final Total: $877.50 ---------------------
    Create the project under /tmp/ecom folder
    ```
7. For the output of the program, you should check the terminal where the Parakett server was run, as we did not configure the event

8. Check the files created at `/tmp/ecom`