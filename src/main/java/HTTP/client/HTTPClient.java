package HTTP.client;

import HTTP.message.exception.*;
import HTTP.message.HTTPRequest;
import HTTP.message.HTTPResponse;
import HTTP.rule.HTTPVersion;
import HTTP.rule.MIME;
import HTTP.rule.MIMETypeNotSupportedException;
import HTTP.utils.EncodingUtil;
import HTTP.utils.FileUtil;
import TCP.TCPClient;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.function.BiConsumer;

public final class HTTPClient extends TCPClient {
    private static final int MAX_REDIRECTS = 5;
    private static final String HOST_NAME = "Simple HTTP Client";
    private final HashMap<String, String> redirectionMap = new HashMap<>();
    private String path;

    public HTTPClient(String host, int port, String path) {
        super(host, port);
        this.path = path;
    }

    public HTTPClient(URL url) {
        super(url);
        path = url.getPath();
    }

    private HTTPResponse getResponse(HTTPRequest request)
            throws IOException, HTTPResponseFormatException, HTTPRequestLineFormatException {
        path = request.getRequestLine().getPath();
        path = redirectionMap.getOrDefault(path, path);
        request.getRequestLine().setPath(path);
        sendMessage(request.getBytes());

        HTTPResponse response = new HTTPResponse(EncodingUtil.decodeBinary(receiveMessage()));

        switch (response.getStatusLine().getStatusCode()) {
            case 301:
                redirectionMap.put(request.getRequestLine().getPath(), response.getHeaders().get("Location"));
            case 302:
                path = response.getHeaders().get("Location");
                request.getRequestLine().setPath(path);
                sendMessage(request.getBytes());
                return new HTTPResponse(EncodingUtil.decodeBinary(receiveMessage()));
            default:
                return response;
        }
    }

    public static void main(String[] args) {

    }

    public void connect(BiConsumer<String, HTTPResponse> handler)
            throws HTTPResponseFormatException, HTTPRequestFormatException, IOException, HTTPMethodNotAllowedException {
        enter("/", handler);
    }

    public void enter(String path, BiConsumer<String, HTTPResponse> handler)
            throws HTTPMethodNotAllowedException, HTTPRequestFormatException,
            HTTPResponseFormatException, IOException {
        if (!isReady()) {
            start();
        }

        HTTPRequest request = new HTTPRequest();
        request.getRequestLine().setMethod("GET");
        request.getRequestLine().setPath(path);
        request.getRequestLine().setVersion(HTTPVersion.getDefaultVersion());
        request.getHeaders().add("Host", HOST_NAME);
        request.getHeaders().add("Content-Length", "0");
        HTTPResponse response = getResponse(request);
        handler.accept(this.path, response);
    }

    public void push(String path, byte[] data, BiConsumer<String, HTTPResponse> handler)
            throws HTTPMethodNotAllowedException, HTTPRequestFormatException,
            HTTPResponseFormatException, IOException, MIMETypeNotSupportedException,
            IllegalArgumentException {
        if (!isReady()) {
            start();
        }

        if (path.endsWith("/")) {
            throw new IllegalArgumentException("Not a file");
        }

        HTTPRequest request = new HTTPRequest();
        request.getRequestLine().setMethod("POST");
        request.getRequestLine().setPath(path);
        request.getRequestLine().setVersion(HTTPVersion.getDefaultVersion());
        request.getHeaders().add("Host", HOST_NAME);
        request.getHeaders().add("Content-Type", MIME.getType(FileUtil.getExtension(path)));
        request.getHeaders().add("Content-Length", String.valueOf(data.length));
        request.getBody().setBody(data);
        handler.accept(this.path, getResponse(request));
    }

//    public static HttpResponse sendRequest(String urlStr, String method, String data, Map<String, String> customHeaders,int redirectCount, boolean keepAlive) throws IOException {
//        if (redirectCount > MAX_REDIRECTS) {
//            throw new IOException("Too many redirects");
//        }
//            // 发送请求
//            if (data != null) {
//                out.print("Content-Type: application/x-www-form-urlencoded\r\n");
//                out.print("Content-Length: " + data.length() + "\r\n");
//            }
//        }
//    }

}