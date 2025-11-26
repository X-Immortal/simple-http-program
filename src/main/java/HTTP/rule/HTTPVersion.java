package HTTP.rule;

import java.util.Collections;
import java.util.LinkedHashSet;

public class HTTPVersion {
    private static final LinkedHashSet<String> supportedVersions = new LinkedHashSet<>();

    static {
        Collections.addAll(supportedVersions, "HTTP/1.1");
    }

    private HTTPVersion() {}

    public static String getDefaultVersion() {
        return supportedVersions.iterator().next();
    }

    public static boolean support(String version) {
        return supportedVersions.contains(version);
    }
}
