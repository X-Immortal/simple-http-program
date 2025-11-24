package CLI;

import HTTP.client.HttpClient;
import TCP.TCPClient;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPClientCLI extends ClientCLI {
    private HttpClient client;

    {
        commands.put("connect", this::connect);
        commands.put("exit", this::exit);
    }

    public static void main(String[] args) throws IOException {
        HTTPClientCLI cli = new HTTPClientCLI();
        cli.start();
    }

    private void connect(org.apache.commons.cli.CommandLine args) {
        String[] argsArr = args.getArgs();
        if (args.hasOption("h")) {
            System.out.println("Usage: connect <url> [-h|--help]");
        } else if (args.getArgs().length == 1) {
            if (client == null || !client.isReady()) {
                URL url;
                try {
                    url = new URL(argsArr[0]);
                } catch (MalformedURLException e) {
                    System.out.println("Invalid url: " + argsArr[0]);
                    return;
                }
                client = new HttpClient(url);
                client.connect();
            }
        } else {
            System.out.println("Invalid arguments");
        }
    }

    void exit(org.apache.commons.cli.CommandLine args) {
        if (client != null && client.isReady()) {
            client.stop();
        }
        System.exit(0);
    }

    @Override
    protected void start() {
        super.start();

        new Thread(() -> {
            while (true) {
                if (client != null && client.isReady()) {
                    try {
                        byte[] message = client.receiveMessage();
                        if (message.length > 0) {
                            System.out.println("Received message: ");
                            System.out.println(new String(message));
                        } else {
                            Thread.sleep(100);
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


}
