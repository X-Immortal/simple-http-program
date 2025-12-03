package CLI.client;

import CLI.Command;
import HTTP.client.HTTPClient;
import HTTP.message.exception.HTTPMethodNotAllowedException;
import HTTP.message.exception.HTTPRequestFormatException;
import HTTP.message.exception.HTTPResponseFormatException;
import HTTP.rule.MIMETypeNotSupportedException;
import HTTP.utils.EncodingUtil;
import HTTP.utils.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

        Command push = new Command(2, "push <filepath> <remote path>", "将<filepath>上传到<remote path>下", this::push);
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

        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            System.out.println("Invalid url: " + urlStr);
            return;
        }
        client = new HTTPClient(url);
        baseURL = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
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
                (client == null || argsArr.length != 0)) {
            System.out.println("Invalid arguments");
            return;
        }
        if (!args.hasOption('f') && !args.hasOption('b') && argsArr.length != 1) {
            System.out.println("Invalid arguments");
            return;
        }

        String path;
        if (args.hasOption('f')) {
            path = this.path + args.getOptionValue('f').replaceAll("^/", "");
        } else if (args.hasOption('b')) {
            path = this.path.replaceAll("/$", "");
            path = path.substring(0, path.lastIndexOf("/") + 1);
        } else {
            if (client == null) {
                connect(argsArr[0]);
            }

            try {
                path = new URL(argsArr[0]).getPath();
            } catch (MalformedURLException e) {
                System.out.println("Invalid url: " + argsArr[0]);
                return;
            }

            if (path.isEmpty()) {
                path = "/";
            }
        }

        if (!client.isReady()) {
            client.start();
        }

        try {
            client.enter(path, (finalPath, response) -> {
                System.out.println("entered:" + baseURL + finalPath);
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

        String[] argsArr = args.getArgs();

        try {
            client.enter(path + argsArr[0], (finalPath, response) -> {
                System.out.println("current: " + baseURL + path);
                if (!finalPath.equals(path + argsArr[0]) ||
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
        if (!path.endsWith("/")) {
            System.out.println("You are not in a directory, cannot use push");
            return;
        }

        String[] argsArr = args.getArgs();
        argsArr[0] = argsArr[0].replaceAll("^\\.[\\\\/]?", CACHE_DIR.replaceAll("\\\\", "\\\\\\\\"));
        argsArr[1] = argsArr[1].replaceAll("^\\.[\\\\/]?", path);
        if (!argsArr[1].endsWith("/")) {
            argsArr[1] += "/";
        }
        String targetPath = argsArr[1] + FileUtil.getName(argsArr[0]);

        try {
            client.push(targetPath, FileUtil.read(argsArr[0]),
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
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
        } catch (MIMETypeNotSupportedException e) {
            System.out.println("Unsupported file type");
        }
    }

    private void login(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;

        int maxTimes = 3;
        String username, password;
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
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
                if (response.getStatusLine().getStatusCode() != 200) {
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
