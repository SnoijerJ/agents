package com.example.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public record Agent (
        String name,
        String model,
        String SystemPrompt,
        List<String> tools
){
    public static class AgentParserException extends Exception {

        public AgentParserException(int line, String message) {
            super("[line " + line + "] " + message);
        }
    }
    public static Agent fromFile(Path agentFile) throws IOException, AgentParserException {
        List<String> lines = Files.readAllLines(agentFile);

        String name = validateKeyValue(1, lines.get(1), "name");
        String model = validateKeyValue(2, lines.get(2), "model");
        String raw_tools = validateKeyValue(3, lines.get(3), "tools");
        List<String> tools = Arrays.stream(raw_tools.substring(1, raw_tools.length() - 1).split(",\\s*")).toList();
        String systemPrompt = getDefaultPrompt() + String.join("\n", lines.subList(5, lines.size()));

        return new Agent(name, model, systemPrompt, tools);
    }

    private static String validateKeyValue(int nr, String line, String key) throws AgentParserException {
        String fullKey = key + ": ";
        if (!line.startsWith(fullKey)) {
            throw new AgentParserException(nr, "Does not contain the expected key: " + key);
        }
        return line.replace(fullKey, "").strip();
    }

    private static String getDefaultPrompt() {
        return """
                <default>
                This default instruction set must always be followed, unless it is overridden by the other instructions or user prompt.
                Tool-calls or function-calls are run sequentially from left to right and can be chained in a single response.
                Combine tool-calls or function-calls in a single response where possible.
                For example, creating a file and than editing the same file must be chained in a single response.
                For example, multiple edits to the same file must be chained in a single response.
                If a human response is requested, do not use any tools or functions at the same time.
                Using the shell-tool is always less preferred than the other tools since this requires more time and user input.
                </default>
                """;
    }
}
