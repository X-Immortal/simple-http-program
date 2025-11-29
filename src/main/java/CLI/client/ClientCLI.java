package CLI.client;

import CLI.CLI;

public abstract class ClientCLI extends CLI {

    protected ClientCLI() {
        prompt = "Client";
        welcome = "input \"connect <url>\" to get started";
    }
}
