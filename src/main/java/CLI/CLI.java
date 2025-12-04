package CLI;

import org.apache.commons.cli.ParseException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;

public abstract class CLI {
    protected String prompt;
    protected String welcome;
    protected String historyPath;
    protected final Terminal terminal;
    protected LineReader reader;
    protected final Parser parser = new DefaultParser();
    protected final HashMap<String, Command> commands = new HashMap<>();

    {
        try {
            terminal = TerminalBuilder.builder()
                    .jansi(true)
                    .jna(true)
                    .system(true)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Command help = new Command(0, "help", "show information of all commands", this::help);
        commands.put("help", help);
    }

    private void init() {
        File file = new File(historyPath);
        file.getParentFile().mkdirs();

        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.HISTORY_FILE, Paths.get(historyPath))
                .variable(LineReader.HISTORY_SIZE, 500)
                .variable(LineReader.HISTORY_FILE_SIZE, 1000)
                .parser(parser)
                .build();
    }

    protected void start() {
        init();

        new Thread(() -> {
            System.out.println("=====" + welcome + "=====");

            while (true) {
                String input;
                input = reader.readLine(prompt + "> ");

                if (input.isEmpty()) continue;
                try {
                    processCommand(parser.parse(input, 0, Parser.ParseContext.COMPLETE));
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid argument");
                }
            }
        }).start();
    }

    protected void processCommand(ParsedLine cmd) {
        String executable = cmd.word();
        if (commands.containsKey(executable)) {
            try {
                List<String> words = cmd.words();
                commands.get(executable).handle(words.subList(1, words.size()).toArray(String[]::new));
            } catch (ParseException e) {
                System.out.println("Invalid arguments");
            }
        } else {
            System.out.println("Invalid command");
        }
    }

    protected void help(org.apache.commons.cli.CommandLine args) {
        StringJoiner joiner = new StringJoiner("\n\n", "Usage:\n", "");
        commands.forEach((name, command) ->
            joiner.add(command.getDescription())
        );
        System.out.println(joiner);
    }
}
