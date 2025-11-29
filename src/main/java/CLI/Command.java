package CLI;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.function.Consumer;

public class Command {
    private final String description;
    private final String usage;
    private final int argsNum;
    private final Consumer<CommandLine> handler;
    private final Options options = new Options();

    {
        options.addOption("h", "help", false, "显示帮助信息");
    }

    public Command(int argsNum, String usage, String description, Consumer<CommandLine> handler) {
        this.argsNum = argsNum;
        this.usage = usage.trim();
        this.description = description;
        this.handler = handler;
    }

    public void addOption(String opt, String longOpt, boolean hasArg, String description) {
        options.addOption(opt, longOpt, hasArg, description);
    }

    public void handle(String[] args) throws ParseException {
        CommandLine arguments = new DefaultParser().parse(options, args);
        if (arguments.hasOption('h')) {
            System.out.println(getDescription());
            return;
        }

        if (arguments.getArgs().length != argsNum) {
            System.out.println("Invalid argument number");
            return;
        }

        handler.accept(arguments);
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(usage);
        options.getOptions().forEach(option ->
                sb.append(" [-")
                        .append(option.getOpt())
                        .append("|--")
                        .append(option.getLongOpt())
                        .append("]")
        );
        options.getOptions().forEach(option ->
                sb.append('\n')
                        .append("  -")
                        .append(option.getOpt())
                        .append("|--")
                        .append(option.getLongOpt())
                        .append(": ")
                        .append(option.getDescription())
        );
        sb.append("\ndescription: ").append(description);
        return sb.toString();
    }
}
