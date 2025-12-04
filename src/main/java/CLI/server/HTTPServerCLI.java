package CLI.server;

import CLI.CLI;
import CLI.Command;
import HTTP.server.HTTPServer;

import java.io.File;

public class HTTPServerCLI extends CLI {
    private HTTPServer server;

    {
        prompt = "Server";
        welcome = "simple http server";
        historyPath = String.join(File.separator, System.getProperty("user.dir"), ".history", "http-server-history.txt");

        commands.put("exit", new Command(0, "exit", "退出服务器", this::exit));
    }

    public static void main(String[] args) {
        HTTPServerCLI cli = new HTTPServerCLI();
        cli.start();
    }

    private void exit(org.apache.commons.cli.CommandLine args) {
        if (server != null && server.isReady()) {
            server.stop();
        }
        System.exit(0);
    }

    @Override
    protected void start() {
        server = new HTTPServer(8019);

        server.setShowStartInfo(port -> {
            System.out.println("server started on port: " + port);
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

        Thread serverThread = new Thread(server::run);
        serverThread.start();

        super.start();
    }
}
