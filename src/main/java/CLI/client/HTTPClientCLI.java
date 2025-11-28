package CLI.client;

import HTTP.client.HTTPClient;
import HTTP.utils.HTTPEncodingUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPClientCLI extends ClientCLI {
    private HTTPClient client;

    {
        commands.put("connect", this::connect);
        commands.put("exit", this::exit);
        commands.put("reconnect", this::reconnect);
        commands.put("enter", this::enter);
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
                connect(argsArr[0]);
                client.connect(message ->
                    System.out.println(HTTPEncodingUtil.decodeText(message))
                );
            } else {
                System.out.println("Already connected");
            }
        } else {
            System.out.println("Invalid arguments");
        }
    }

    private void connect(String urlStr) {
        if (isReady()) {
            return;
        }

        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            System.out.println("Invalid url: " + urlStr);
            return;
        }
        client = new HTTPClient(url);
    }

    private void reconnect(org.apache.commons.cli.CommandLine args) {
        String[] argsArr = args.getArgs();
        if (args.hasOption("h")) {
            System.out.println("Usage: reconnect [-h|--help]");
        } else if (args.getArgs().length == 0) {
            if (!isReady()) {
                System.out.println("Did not have a connection");
                return;
            }

            client.connect(message ->
                    System.out.println(HTTPEncodingUtil.decodeText(message))
            );
        } else {
            System.out.println("Invalid arguments");
        }
    }

    private void enter(org.apache.commons.cli.CommandLine args) {
        String[] argsArr = args.getArgs();
        if (client == null) {
            connect(argsArr[0]);
        }
        if (!client.isReady()) {
            client.start();
        }
        try {
            URL url = new URL(argsArr[0]);
            client.enter(url.getPath(), message ->
                    System.out.println(HTTPEncodingUtil.decodeText(message))
            );
        } catch (MalformedURLException e) {
            System.out.println("Invalid url: " + argsArr[0]);
        }
    }

    void exit(org.apache.commons.cli.CommandLine args) {
        if (client != null && client.isReady()) {
            client.stop();
        }
        System.exit(0);
    }

    private boolean isReady() {
        return client != null && client.isReady();
    }
}
