package CLI.server;

import CLI.CLI;

public abstract class ServerCLI extends CLI {

    protected ServerCLI() {
        prompt = "Server";
        welcome = "simple server";
    }
}
