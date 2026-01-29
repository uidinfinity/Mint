package net.melbourne.utils.miscellaneous;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {
    public static void resetFile(String path) throws IOException {
        if (Files.exists(Paths.get(path)))
            new File(path).delete();

        Files.createFile(Paths.get(path));
    }

    public static void createDirectory(String path) throws IOException {
        if (!Files.exists(Paths.get(path)))
            Files.createDirectories(Paths.get(path));
    }

    public static boolean fileExists(String path) {
        return Files.exists(Paths.get(path));
    }

    public static List<String> readLines(File file) {
        List<String> messages = new ArrayList<>();
        if (file.exists()) {
            try (Stream<String> lines = Files.lines(Paths.get(file.getPath()))) {
                messages = lines.filter(l -> !l.isEmpty()).toList();
            } catch (Exception ignored) {
            }
        }
        return messages;
    }
}