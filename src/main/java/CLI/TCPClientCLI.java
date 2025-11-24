package CLI;

import TCP.TCPClient;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class TCPClientCLI extends ClientCLI {
    private TCPClient client;

    {
        commands.put("connect", this::connect);
        commands.put("send", this::send);
        commands.put("exit", this::exit);
    }

    public static void main(String[] args) throws IOException {
        TCPClientCLI cli = new TCPClientCLI();
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
                client = new TCPClient(url);
                client.start();
            }
        } else {
            System.out.println("Invalid arguments");
        }
    }

    private void send(org.apache.commons.cli.CommandLine args) {
        String[] argsArr = args.getArgs();
        if (args.hasOption("h")) {
            System.out.println("Usage: send <message> [-h|--help]");
        } else if (args.getArgs().length == 1) {
            if (client == null || !client.isReady()) {
                System.out.println("Error: connection closed");
                return;
            }
            try {
                client.sendMessage(argsArr[0].replaceAll("\"", "").getBytes());
                System.out.println("Succeeded to send message to server");
            } catch (IOException e) {
                System.out.println("Failed to send message to server");
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
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
