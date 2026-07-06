package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    public static Map<String, String> parseFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);

        int startMeta = -1;
        int endMeta = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if ("---".equals(line.strip())) {
                if (startMeta == -1) {
                    startMeta = i;
                } else {
                    endMeta = i;
                    break;
                }
            }
        }

        if (endMeta == -1) {
            throw new IOException(file + " can not be parsed, missing meta data");
        }

        Map<String, String> result = new HashMap<>();
        for (int i = startMeta + 1; i < endMeta; i++) {
            String[] line = lines.get(i).split(": ", 2);
            result.put(line[0], line[1]);
        }
        result.put("content", String.join("\n", lines.subList(endMeta, lines.size())));
        return result;
    }
}
