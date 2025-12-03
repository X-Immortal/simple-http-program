package CLI.client;

import CLI.Command;
import HTTP.client.HTTPClient;
import HTTP.message.exception.HTTPMethodNotAllowedException;
import HTTP.message.exception.HTTPRequestFormatException;
import HTTP.message.exception.HTTPResponseFormatException;
import HTTP.rule.MIMETypeNotSupportedException;
import utils.EncodingUtil;
import utils.FileUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPClientCLI extends ClientCLI {
    private HTTPClient client;
    private String path;
    private String baseURL;
    private final String CACHE_DIR;

    {
        CACHE_DIR = System.getProperty("user.dir") + File.separator + ".cache" + File.separator;
        File file = new File(CACHE_DIR);
        file.mkdirs();

        commands.put("exit", new Command(0, "exit", "退出程序", this::exit));
        commands.put("login", new Command(0, "login", "登录", this::login));
        commands.put("logout", new Command(0, "logout", "退出登录", this::logout));
        commands.put("register", new Command(0, "register", "注册", this::register));

        Command refresh = new Command(0, "refresh", "刷新当前页面", this::refresh);
        refresh.addOption("r", "root", false, "enter the page by root privilege");
        commands.put("refresh", refresh);

        Command enter = new Command(-1, "enter <url>", "进入<url>指定的页面", this::enter);
        enter.addOption("f", "forward", true, "forward to the next page");
        enter.addOption("b", "back", false, "back to the previous page");
        enter.addOption("r", "root", false, "enter the page by root privilege");
        commands.put("enter", enter);

        Command fetch = new Command(1, "fetch <filename>", "从当前页面下载<filename>文件", this::fetch);
        fetch.addOption("r", "root", false, "enter the page by root privilege");
        commands.put("fetch", fetch);

        Command push = new Command(2, "push <filepath> <remote path>", "将<filepath>指示的文件上传到<remote path>下，<remote path>只能为相对路径", this::push);
        push.addOption("r", "root", false, "enter the page by root privilege");
        commands.put("push", push);
    }

    public static void main(String[] args) {
        HTTPClientCLI cli = new HTTPClientCLI();
        cli.start();
    }

    private void connect(String urlStr) {
        if (isReady()) {
            return;
        }

        try {
            URL url = new URL(urlStr);
            client = new HTTPClient(url);
            baseURL = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
        } catch (MalformedURLException e) {
            System.out.println("Invalid url: " + urlStr);
        }
    }

    private void refresh(org.apache.commons.cli.CommandLine args) {
        if (baseURL == null) {
            System.out.println("Did not have a connection");
            return;
        }

        try {
            client.enter(path, (path, response) -> {
                System.out.println("entered: " + baseURL + path);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
            }, args.hasOption("r"));
        } catch (HTTPResponseFormatException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
        } catch (IOException e) {
            connect(baseURL);
            if (!isReady()) {
                System.out.println("transmission failed");
            } else {
                refresh(args);
            }
        }
    }

    private void enter(org.apache.commons.cli.CommandLine args) {
        if (args.hasOption('f') && args.hasOption('b')) {
            System.out.println("Invalid options");
            return;
        }

        String[] argsArr = args.getArgs();
        if ((args.hasOption('f') || args.hasOption('b')) &&
                (!isReady() || argsArr.length != 0)) {
            System.out.println("Invalid arguments");
            return;
        }
        if (!args.hasOption('f') && !args.hasOption('b') && argsArr.length != 1) {
            System.out.println("Invalid arguments");
            return;
        }

        String path;
        if (args.hasOption('f')) {
            path = this.path.replaceAll("(?<!/)$", "/") + args.getOptionValue('f').replaceAll("^/", "");
        } else if (args.hasOption('b')) {
            if (this.path.equals("/")) {
                System.out.println("You are already in the root directory, cannot get back");
                return;
            }
            path = this.path.replaceAll("/$", "");
            path = path.substring(0, path.lastIndexOf("/") + 1);
        } else {
            try {
                path = new URL(argsArr[0]).getPath();
            } catch (MalformedURLException e) {
                System.out.println("Invalid url: " + argsArr[0]);
                return;
            }

            if (!isReady()) {
                connect(argsArr[0]);
            }

            if (path.isEmpty()) {
                path = "/";
            }
        }

        try {
            client.enter(path, (finalPath, response) -> {
                System.out.println("entered: " + baseURL + finalPath);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                this.path = finalPath;
            }, args.hasOption("r"));
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
        }
    }

    private void fetch(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;
        if (!path.startsWith("/document")) {
            System.out.println("'fetch' is invalid in this page");
            return;
        }

        String[] argsArr = args.getArgs();
        if (argsArr[0].indexOf('/') != -1) {
            System.out.println("Invalid argument");
            return;
        }
        String targetPath = path.replaceAll("(?<!/)$", "/") + argsArr[0];

        try {
            client.enter(targetPath, (finalPath, response) -> {
                System.out.println("current: " + baseURL + path);
                if (!finalPath.equals(targetPath) ||
                        response.getStatusLine().getStatusCode() != 200) {
                    System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                    System.out.println("Fetch " + baseURL + finalPath + " failed");
                    return;
                }
                try {
                    FileUtil.write(CACHE_DIR + argsArr[0], response.getBody().getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Fetch " + baseURL + finalPath + " successfully");
                System.out.println("You can view it in " + CACHE_DIR);
            }, args.hasOption("r"));
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
        }
    }

    private void push(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;
        if (!path.startsWith("/document")) {
            System.out.println("'push' is invalid in this page");
            return;
        }

        String[] argsArr = args.getArgs();
        String targetPath = argsArr[1] + "/" + FileUtil.getName(argsArr[0]);

        try {
            client.push(new URL(targetPath).getPath(), FileUtil.read(CACHE_DIR, argsArr[0]),
                    (finalPath, response) -> {
                        path = finalPath;
                        System.out.println("entered: " + baseURL + finalPath);
                        System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                        if (!finalPath.equals(targetPath) ||
                                response.getStatusLine().getStatusCode() != 200) {
                            System.out.println("Push " + argsArr[0] + " failed");
                            return;
                        }
                        System.out.println("Push " + argsArr[0] + " successfully");
                    }, args.hasOption("r"));
        } catch (HTTPResponseFormatException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
        } catch (MIMETypeNotSupportedException e) {
            System.out.println("Unsupported file type");
        } catch (MalformedURLException e) {
            System.out.println("Invalid url: " + targetPath);
        } catch (FileNotFoundException e) {
            System.out.println("Unexisted file: " + argsArr[0]);
        } catch (IOException e) {
            System.out.println("Failed to read file: " + argsArr[0]);
        }
    }

    private void login(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;

        int maxTimes = 3;
        String username, password;
        Console console = System.console();
        if (console == null) {
            System.out.println("Client error");
            return;
        }

        while (true) {
            if (--maxTimes < 0) {
                System.out.println("Too many attempts");
                return;
            }
            username = console.readLine("your username: ");
            if (username.isEmpty()) {
                continue;
            }
            password = new String(console.readPassword("your password: "));
            if (password.isEmpty()) {
                continue;
            }
            break;
        }

        try {
            client.login(username, password, (finalPath, response) -> {
                path = finalPath;
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                    System.out.println("Login failed");
                    return;
                }
                System.out.println("Login successfully");
                System.out.println("current: " + baseURL + path);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
            });
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException | MIMETypeNotSupportedException e) {
            System.out.println("Client error");
        }
    }

    private void register(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;

        int maxTimes = 3;
        String username, password, confirm;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            if (--maxTimes < 0) {
                System.out.println("Too many attempts");
                return;
            }
            try {
                System.out.print("your username: ");
                username = br.readLine();
                if (username.isEmpty()) {
                    continue;
                }
                System.out.print("your password: ");
                password = br.readLine();
                if (password.isEmpty()) {
                    continue;
                }
                System.out.print("confirm your password: ");
                confirm = br.readLine();
                if (!password.equals(confirm)) {
                    System.out.println("Password does not match");
                    continue;
                }
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            client.register(username, password, (finalPath, response) -> {
                path = finalPath;
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.out.println("Register failed");
                    System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                    return;
                }
                System.out.println("Register successfully");
                System.out.println("current: " + baseURL + path);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
            });
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException | MIMETypeNotSupportedException e) {
            System.out.println("Client error");
        }
    }

    private void logout(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;

        try {
            client.logout((finalPath, response) -> {
                path = finalPath;
                if (!path.equals("/") || response.getStatusLine().getStatusCode() != 200) {
                    System.out.println("Logout failed");
                    System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                    return;
                }
                System.out.println("Logout successfully");
                System.out.println("current: " + baseURL + path);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
            });
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException | MIMETypeNotSupportedException e) {
            System.out.println("Client error");
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

    private boolean checkConnection() {
        if (!isReady()) {
            System.out.println("Did not have a connection");
            return false;
        }
        return true;
    }
}
