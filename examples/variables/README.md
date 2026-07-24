#Variables

Variables here work just like variables in programming languages. Placeholders in the prompt instructions, formatted as `{{variable_name}}`, undergo simple string replacement with their actual values before being sent to the LLM. The available variables are defined directly within the agent's definition. Please note that the replacement is done by server, not by model. 

As an example, we’ll look at a use case where an agent prompts the LLM to send order details to a customer. While this could easily be automated with a simple script, it serves as a straightforward placeholder until we find a better example. We will also be using agent test API to trigger the event using REST. Note that we are sending customer information to LLM. In real scenario you will not be sending customer information like email to model. You may be sending masked information.

# Instructions

1. Ensure you have build GMail and SQlite MCP servers, Parakett Server
2. Ensure you have setup the appropriate credentials for Gmail channel. Refer the [instructions](https://vittoda.github.io/parakett/creds/) for an example.
3. Setup SQLite DB. Use any DB tool (like [sqlectron](https://sqlectron.github.io/) ) and following the instruction below to setup our customer DB
    1. Create a DB `variables_example`. You need to create it in `/tmp` folder, as the MCP configuration use this folder. Change it appropriately if you are using something else.
    2. Create two tables   `customers` and `orders` using the following DDLs

        **Customer table**
        ```
        CREATE TABLE customer (
            customer_id INTEGER PRIMARY KEY,
            customer_email TEXT NOT NULL,
            full_name TEXT NOT NULL
        );
        ```

         **Orders table**
        ```
        CREATE TABLE orders (
            order_id INTEGER PRIMARY KEY,
            customer_id INTEGER NOT NULL,
            item_name TEXT NOT NULL,
            status TEXT NOT NULL
        );
        ```
    3. Add sample customers and orders table. Replace the email id with your email id.
        ```
        INSERT INTO customer (customer_id, customer_email, full_name) 
            VALUES 
                (101, 'demo1@example.com', 'Alice Smith'),
                (102, 'demo2@example.com', 'Bob Jones');
        ```

        ```
        INSERT INTO orders (order_id, customer_id, item_name, status) 
            VALUES 
                (5001, 101, 'Wireless Mouse', 'Shipped'),
                (5002, 101, 'Mechanical Keyboard', 'Processing'),
                (5003, 102, '27-inch Monitor', 'Delivered'),
                (5004, 102, 'USB-C Cable', 'Shipped');
        ```

4. Start the followstack server. Replace the `mcp_server_folder` with the correct root folder for MCP server repository.
    ```
       java -Dfs.mcpConfigFile=./examples/variables/mcpServers.json \
        -Dfs.agentsConfigFile=./examples/variables/agents.json \
        -Dfs.channelsConfigFile=./examples/variables/channelsConfig.json \
        -Dmcp.base=<mcp_server_folder> \
        -DopenAI.model.logRequests=true \
        -jar build/libs/parakett-0.0.1.jar
    ```

5. Use curl command to send the prompt. Please note the variables.
    ```
    curl -X POST http://localhost:8080/fs/api/v1/agents/sqlite_and_gmail/testrun \
    -H "Content-Type: application/json" \
    -d '{
        "enableDebug": false,
        "enableLog": true,
        "enableArchive": true,
        "input": "Fetch the order status for customer id {{customer_id}} for order {{order_id}} and send email to customers email address in your records.",
        "variables": [
        {
            "name": "customer_id",
            "value": "102"
        },
        {
            "name": "order_id",
            "value": "5004"
        }
        ]
    }'
    ```

    You should see an email in your mailbox about the status of the order. Email format and error handling will depend on the prompt and domain context. You can imrpove the same by updating agent domain context file `gmailAndSqliteContext.txt`