package CLI.server;

import HTTP.server.HTTPServer;

public class HTTPServerCLI extends ServerCLI {
    private HTTPServer server;

    {
        commands.put("exit", this::exit);
    }

    public static void main(String[] args) {
        HTTPServerCLI cli = new HTTPServerCLI();
        cli.start();
    }

    void exit(org.apache.commons.cli.CommandLine args) {
        if (server != null && server.isReady()) {
            server.stop();
        }
        System.exit(0);
    }

    @Override
    protected void start() {
        super.start();

        server = new HTTPServer(8080);

        server.setShowStartInfo(port -> {
            System.out.println();
            System.out.println("server started on port: " + port);
            printPrompt();
        });

        server.setShowReceivedMessage(message ->{
            System.out.println();
            System.out.println("Received request message:");
            System.out.println(new String(message));
            printPrompt();
        });

        server.setShowSentMessage(message -> {
            System.out.println();
            System.out.println("Replied response message:");
            System.out.println(new String(message));
            printPrompt();
        });

        server.start();

        if (!server.isReady())  throw new RuntimeException("Failed to start server");

        Thread serverThread = new Thread(server::run);
        serverThread.start();
    }
}
