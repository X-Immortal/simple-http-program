package CLI.server;

import CLI.CLI;
import CLI.Command;
import HTTP.server.HTTPServer;
import utils.EncodingUtil;

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
            reader.printAbove("server started on port: " + port + "\n");
        });

        server.setShowReceivedMessage(message ->{
            reader.printAbove("Received request message:\n");
            reader.printAbove(EncodingUtil.decodeText(message) + "\n\n");
        });

        server.setShowSentMessage(message -> {
            reader.printAbove("Replied response message:\n");
            reader.printAbove(EncodingUtil.decodeText(message) + "\n\n");
        });

        Thread serverThread = new Thread(server::run);
        serverThread.start();

        super.start();
    }
}
