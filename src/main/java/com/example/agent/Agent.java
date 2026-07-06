package com.example.agent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.example.Utils.parseFile;

public record Agent (
        String name,
        String model,
        String SystemPrompt,
        List<String> tools
){
    public static Agent fromFile(Path agentFile) throws IOException, AgentException {
        Map<String, String> agentMap = parseFile(agentFile);

        String raw_tools = validateKeyValue(agentMap, "tools");
        List<String> tools = Arrays.stream(raw_tools.substring(1, raw_tools.length() - 1).split(",\\s*")).toList();

        return new Agent(validateKeyValue(agentMap, "name"),
                validateKeyValue(agentMap, "model"),
                validateKeyValue(agentMap, "content"),
                tools);
    }

    private static String validateKeyValue(Map<String, String> agentMap, String key) throws AgentException {
        if (!agentMap.containsKey(key) || agentMap.get(key).isBlank()) {
            throw new AgentException("Does not contain the expected key with a value: " + key);
        }
        return agentMap.get(key);
    }

    public static String getDefaultPrompt() {
        return """
                <default>
                This default instruction set must always be followed, unless it is overridden by the other instructions or user prompt.
                Tool-calls or function-calls are run sequentially from left to right and can be chained in a single response.
                Combine tool-calls or function-calls in a single response where possible.
                For example, creating a file and than editing the same file must be chained in a single response.
                For example, multiple edits to the same file must be chained in a single response.
                If a human response is requested, do not use any tools or functions at the same time.
                You must use other tools than the shell tool wherever possible since this requires more time and user input.
                Do not create long chained shell commands.
                </default>""";
    }
}
