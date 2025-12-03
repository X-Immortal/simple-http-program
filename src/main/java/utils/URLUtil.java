package utils;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class URLUtil {
    private URLUtil() {}

    public static String normalize(String path) throws InvalidPathException {
        return Paths.get(path).normalize().toString().replaceAll("\\\\", "/");
    }
}
