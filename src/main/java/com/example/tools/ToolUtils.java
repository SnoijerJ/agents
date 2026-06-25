package com.example.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ToolUtils {

    public static boolean isWithinDirectory(Path baseDir, Path target) {
        Path normalizedBase = baseDir.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();

        return normalizedTarget.startsWith(normalizedBase);
    }

    public static Map<String, String> runCommand(String command) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(new String[]{"/bin/bash", "-c", command});

        Process process = pb.start();

        Map<String, String> result = new HashMap<>();
        result.put("output", new String(process.getInputStream().readAllBytes()));
        result.put("err", new String(process.getErrorStream().readAllBytes()));
        result.put("exit_code", String.valueOf(process.waitFor()));

        return result;
    }
}
