package CLI.client;

import CLI.Command;
import TCP.TCPClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class TCPClientCLI extends ClientCLI {
    private TCPClient client;

    {
        commands.put("connect", new Command(1, "connect <url>", "连接到<url>指向的服务器", this::connect));
        commands.put("send", new Command(1, "send <message>", "发送<message>给服务器，并显示服务器的答复", this::send));
        commands.put("exit", new Command(0, "exit", "退出程序", this::exit));
    }

    public static void main(String[] args) {
        TCPClientCLI cli = new TCPClientCLI();
        cli.start();
    }

    private void connect(org.apache.commons.cli.CommandLine args) {
        String[] argsArr = args.getArgs();
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
    }

    private void send(org.apache.commons.cli.CommandLine args) {
        String[] argsArr = args.getArgs();
        if (client == null || !client.isReady()) {
            System.out.println("Error: connection closed");
            return;
        }

        try {
            client.sendMessage(argsArr[0].getBytes());
            System.out.println("Succeeded to send message to server");
        } catch (IOException e) {
            System.out.println("Failed to send message to server");
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
