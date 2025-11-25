package CLI.server;

import CLI.CLI;

public abstract class ServerCLI extends CLI {

    protected ServerCLI() {
        name = "Server";
        welcome = "simple server";
    }
}
