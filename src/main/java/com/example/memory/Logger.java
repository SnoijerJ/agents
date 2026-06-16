package com.example.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class Logger {
    Path LOGGING_DIR = Path.of(".ai/ai_log");
    String LOG_FILE_PREFIX = "conversation-";

    public void createLogFile() throws IOException {
        Path folder = getTodayFolder();
        Files.createDirectories(folder);
        Path file = Path.of(folder.toString(), logFileName(getLastLogInt() + 1));
        Files.createFile(file);
    }

    public void append(String log) throws IOException {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String prefix = "[" + time + "] ";
        String result = String.join("\n", Arrays.stream(log.split("\n"))
                .map(l -> prefix + l)
                .toList());
        Files.write(getLastLogFile(), result.getBytes(),  StandardOpenOption.APPEND);
    }

    private Path getTodayFolder() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return Path.of(LOGGING_DIR.toString(), date);
    }

    private int getLastLogInt() throws IOException {
        Path folder = getTodayFolder();
        List<Integer> logList = Files.list(folder).filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(f -> f.startsWith(LOG_FILE_PREFIX))
                .map(f -> Integer.parseInt(f.replace(LOG_FILE_PREFIX, "").split("\\.")[0]))
                .sorted()
                .toList();
        int lastLog = -1;
        if (!logList.isEmpty()) {
            lastLog = logList.get(logList.size() -1);
        }

        return lastLog;
    }

    private Path getLastLogFile() throws IOException {
        return Path.of(getTodayFolder().toString(), logFileName(getLastLogInt()));
    }

    private String logFileName(int number) {
        return LOG_FILE_PREFIX + number + ".log";
    }
}
