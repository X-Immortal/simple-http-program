package CLI;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.function.Consumer;

public abstract class CLI {
    protected String name;
    protected String welcome;
    protected final Options options = new Options();
    protected final CommandLineParser parser = new DefaultParser();
    protected final HashMap<String, Consumer<CommandLine>> commands = new HashMap<>();

    {
        options.addOption("h", "help", false, "显示帮助信息");
    }

    protected void start() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        new Thread(() -> {
            System.out.println("=====" + welcome + "=====");

            while (true) {
                printPrompt();
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

    protected void printPrompt() {
        System.out.print(name + "> ");
    }
}
