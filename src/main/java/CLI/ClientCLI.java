package CLI;

import TCP.TCPClient;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.function.Consumer;

public abstract class ClientCLI {
    protected final Options options = new Options();
    protected final CommandLineParser parser = new DefaultParser();
    protected final HashMap<String, Consumer<CommandLine>> commands = new HashMap<>();

    {
        options.addOption("h", "help", false, "显示帮助信息");
    }

    protected void start() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        new Thread(() -> {
            System.out.println("====input \"connect <url>\" to get start====");

            while (true) {
                System.out.print("Client> ");
                String input;
                try {
                    input = br.readLine().trim();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (input.isEmpty()) continue;
                processCommand(org.apache.commons.exec.CommandLine.parse(input));
            }
        }).start();
    }

    protected void processCommand(org.apache.commons.exec.CommandLine cmd) {
        String executable = cmd.getExecutable();
        if (commands.containsKey(executable)) {
            org.apache.commons.cli.CommandLine arguments;
            try {
                arguments = parser.parse(options, cmd.getArguments());
            } catch (ParseException e) {
                System.out.println("Invalid arguments");
                return;
            }
            commands.get(executable).accept(arguments);
        }
    }
}
