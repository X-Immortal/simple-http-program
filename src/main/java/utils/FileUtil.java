package utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.TimeZone;

public class FileUtil {
    private FileUtil() {}

    public static String getAbsolutePath(String workDir, String path) throws FileNotFoundException {
        File file = new File(workDir, path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
        return file.getAbsolutePath();
    }

    public static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1);
    }

    public static String getName(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            return "";
        }
        int slashIndex = path.lastIndexOf(File.separator);
        return slashIndex == -1 ? path : path.substring(slashIndex + 1);
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


    public static String listFiles(String path, FileFilter filter) {
        StringJoiner joiner = new StringJoiner("\n");
        File file = new File(path);
        File[] files = file.listFiles(filter);
        if (files == null) return "";
        for (File f : files) {
            joiner.add(f.getName());
        }
        return joiner.toString();
    }

    public static String listFiles(String path) {
        StringJoiner joiner = new StringJoiner("\n");
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) return "";
        for (File f : files) {
            joiner.add(f.getName());
        }
        return joiner.toString();
    }

    public static byte[] read(String workDir, String path) throws IOException {
        return read(getAbsolutePath(workDir, path));
    }

    public static byte[] read(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
        if (file.isDirectory()) {
            throw new IOException("Not a file");
        }
        try (FileInputStream fis = new FileInputStream(path)) {
            return fis.readAllBytes();
        }
    }

    public static void write(String path, byte[] content) throws IOException {
        File file = new File(path);
        if (file.isDirectory()) {
            throw new IOException("Not a file");
        }

        new File(file.getParent()).mkdirs();
        file.createNewFile();

        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(content);
        }
    }
}
