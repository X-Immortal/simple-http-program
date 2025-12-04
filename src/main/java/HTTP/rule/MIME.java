package HTTP.rule;

import java.util.HashMap;
import java.util.Map;

public class MIME {
    private static final Map<String, String> typeMap = new HashMap<>();

    static {
        typeMap.put("html", "text/html");
        typeMap.put("txt", "text/plain");
        typeMap.put("json", "application/json");
        typeMap.put("jpg", "image/jpeg");
        typeMap.put("png", "image/png");
    }

    private MIME() {}

    public static String getType(String extension) throws MIMETypeNotSupportedException {
        if (!support(extension)) {
            throw new MIMETypeNotSupportedException();
        }
        return typeMap.getOrDefault(extension, "text/plain");
    }

    public static boolean support(String extension) {
        return typeMap.containsKey(extension);
    }

    public static boolean isText(String type) {
        return type.startsWith("text/");
    }
}
