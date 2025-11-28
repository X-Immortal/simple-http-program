package HTTP.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HTTPEncodingUtil {
    public static final Charset TEXT_CHARSET = StandardCharsets.UTF_8;
    public static final Charset BINARY_CHARSET = StandardCharsets.ISO_8859_1;

    private HTTPEncodingUtil() {}

    public static String textToBinary(String text) {
        return new String(text.getBytes(TEXT_CHARSET), BINARY_CHARSET);
    }

    public static String binaryToText(String binary) {
        return new String(binary.getBytes(BINARY_CHARSET), TEXT_CHARSET);
    }

    public static byte[] encodeText(String text) {
        return text.getBytes(TEXT_CHARSET);
    }

    public static String decodeText(byte[] bytes) {
        return new String(bytes, TEXT_CHARSET);
    }

    public static byte[] encodeBinary(String binary) {
        return binary.getBytes(BINARY_CHARSET);
    }

    public static String decodeBinary(byte[] bytes) {
        return new String(bytes, BINARY_CHARSET);
    }
}
