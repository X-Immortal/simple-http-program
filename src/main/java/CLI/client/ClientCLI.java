package CLI.client;

import CLI.CLI;

public abstract class ClientCLI extends CLI {

    protected ClientCLI() {
        name = "Client";
        welcome = "input \"connect <url>\" to get started";
    }
}
