package HTTP.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FileUtil {

    private FileUtil() {}

    public static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1);
    }

    public static String getTimestamp(String path) throws FileNotFoundException {
        File file = new File(path);

        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException(path);
        }

        Date lastModified = new Date(file.lastModified());

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(lastModified);
    }

    public static String listFiles(String path) {
        StringBuilder sb = new StringBuilder();
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) return "";
        for (File f : files) {
            sb.append(f.getName()).append("\n");
        }
        return sb.toString();
    }

    public static byte[] read(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, HTTPEncodingUtil.BINARY_CHARSET));
            }
            return HTTPEncodingUtil.encodeBinary(sb.toString());
        }
    }
}
