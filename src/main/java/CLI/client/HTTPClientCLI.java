package CLI.client;

import HTTP.client.HTTPClient;
import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPClientCLI extends ClientCLI {
    private HTTPClient client;

    {
        commands.put("connect", this::connect);
        commands.put("exit", this::exit);
        commands.put("reconnect", this::reconnect);
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
            if (!isReady()) {
                URL url;
                try {
                    url = new URL(argsArr[0]);
                } catch (MalformedURLException e) {
                    System.out.println("Invalid url: " + argsArr[0]);
                    return;
                }
                client = new HTTPClient(url);
                client.connect();
            } else {
                System.out.println("Already connected");
            }
        } else {
            System.out.println("Invalid arguments");
        }
    }

    private void reconnect(org.apache.commons.cli.CommandLine args) {
        if (!isReady()) {
            System.out.println("Did not have a connection");
            return;
        }

        client.connect();
    }

    private boolean isReady() {
        return client != null && client.isReady();
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

        Thread receiveThread = new Thread(() -> {
            while (true) {
                if (client != null && client.isReady()) {
                    try {
                        byte[] message = client.receiveMessage();
                        if (message.length > 0) {
                            System.out.println();
                            System.out.println("Received message: ");
                            System.out.println(new String(message));
                            System.out.print("Client> ");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        receiveThread.start();
    }
}
