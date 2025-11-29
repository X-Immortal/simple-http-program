package CLI;

import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringJoiner;

public abstract class CLI {
    protected String prompt;
    protected String welcome;
    protected final HashMap<String, Command> commands = new HashMap<>();

    {
        Command help = new Command(0, "help", "show information of all commands", this::help);
        commands.put("help", help);
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
            try {
                commands.get(executable).handle(cmd.getArguments());
            } catch (ParseException e) {
                System.out.println("Invalid arguments");
            }
        } else {
            System.out.println("Invalid command");
        }
    }

    protected void printPrompt() {
        System.out.print(prompt + "> ");
    }

    protected void help(org.apache.commons.cli.CommandLine args) {
        StringJoiner joiner = new StringJoiner("\n\n", "Usage:\n", "");
        commands.forEach((name, command) ->
            joiner.add(command.getDescription())
        );
        System.out.println(joiner);
    }
}
