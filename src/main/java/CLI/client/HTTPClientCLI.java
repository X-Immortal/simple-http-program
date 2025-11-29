package CLI.client;

import CLI.Command;
import HTTP.client.HTTPClient;
import HTTP.message.exception.HTTPMethodNotAllowedException;
import HTTP.message.exception.HTTPRequestFormatException;
import HTTP.message.exception.HTTPResponseFormatException;
import HTTP.rule.MIMETypeNotSupportedException;
import HTTP.utils.EncodingUtil;
import HTTP.utils.FileUtil;

import java.io.File;
import java.io.IOException;
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

        commands.put("connect", new Command(1, "connect <url>", "连接到<url>指向的服务器", this::connect));
        commands.put("exit", new Command(0, "exit", "退出程序", this::exit));
        commands.put("refresh", new Command(0, "refresh", "刷新当前页面", this::refresh));
        commands.put("enter", new Command(1, "enter <url>", "进入<url>指定的页面", this::enter));
        commands.put("login", new Command(0, "login", "登录", this::login));
        commands.put("register", new Command(0, "register", "注册", this::register));
        commands.put("fetch", new Command(1, "fetch <filename>", "从当前页面下载<filename>文件", this::fetch));
        commands.put("push", new Command(2, "push <filepath> <remote path>", "将<filepath>上传到<remote path>下", this::push));
    }

    public static void main(String[] args) {
        HTTPClientCLI cli = new HTTPClientCLI();
        cli.start();
    }

    private void connect(org.apache.commons.cli.CommandLine args) {
        if (isReady()) {
            System.out.println("Already connected");
        }

        String[] argsArr = args.getArgs();
        connect(argsArr[0]);
        try {
            client.connect((path, response) -> {
                System.out.println("entered: " + baseURL + path);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                this.path = path;
            });
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
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
        baseURL = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
    }

    private void refresh(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;

        try {
            client.enter(path, (path, response) -> {
                System.out.println("entered: " + baseURL + path);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
            });
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
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
            String path = url.getPath();
            if (path.isEmpty()) {
                path = "/";
            }
            client.enter(path, (finalPath, response) -> {
                System.out.println("entered:" + baseURL + finalPath);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                this.path = finalPath;
            });
        } catch (MalformedURLException e) {
            System.out.println("Invalid url: " + argsArr[0]);
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            System.out.println("Client error");
        }
    }

    private void fetch(org.apache.commons.cli.CommandLine args) {
        if (!checkConnection()) return;
        if (!path.endsWith("/")) {
            System.out.println("You are not in a directory, cannot use fetch");
            return;
        }

        String[] argsArr = args.getArgs();
        if (argsArr[0].equals("/")) {
            System.out.println("Invalid filename");
            return;
        }

        try {
            client.enter(path + argsArr[0], (finalPath, response) -> {
                System.out.println("current: " + baseURL + path);
//                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                if (response.getStatusLine().getStatusCode() != 200) {
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
            });
        } catch (HTTPResponseFormatException | IOException e) {
//            e.printStackTrace();
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
        if (argsArr[0].endsWith("/")) {
            System.out.println("Invalid filepath");
            return;
        }
        if (!argsArr[1].endsWith("/")) {
            argsArr[1] += "/";
        }

        try {
            client.push(argsArr[1] + FileUtil.getName(argsArr[0]), FileUtil.read(argsArr[0]),
                    (finalPath, response) -> {
                System.out.println("entered: " + baseURL + finalPath);
                System.out.println(EncodingUtil.decodeText(response.getBody().getBytes()));
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.out.println("Push " + argsArr[0] + " failed");
                    return;
                }
                System.out.println("Push " + argsArr[0] + " successfully");
            });
        } catch (HTTPResponseFormatException | IOException e) {
            System.out.println("transmission failed");
        } catch (HTTPRequestFormatException | HTTPMethodNotAllowedException e) {
            e.printStackTrace();
            System.out.println("Client error");
        } catch (MIMETypeNotSupportedException e) {
            System.out.println("Unsupported file type");
        }
    }

    private void login(org.apache.commons.cli.CommandLine args) {
        System.out.println("Not implemented");
    }

    private void register(org.apache.commons.cli.CommandLine args) {
        System.out.println("Not implemented");
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
