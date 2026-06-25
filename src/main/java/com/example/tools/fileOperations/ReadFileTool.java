package com.example.tools.fileOperations;

import com.example.memory.Logger;
import com.example.tools.Parameter;
import com.example.tools.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.example.tools.ToolUtils.isWithinDirectory;

public class ReadFileTool implements Tool {

    @Override
    public String getName() {
        return "read_file";
    }

    @Override
    public String getDescription() {
        return "Reads a file from disk and returns its content.";
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
            new Parameter("file_path", "string", "Path to the file to read")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Path path = Path.of((String) args.get("file_path"));

            System.out.println("[SYSTEM] Reading file: " + path);

            if (!isWithinDirectory(Path.of("./"), path)) {
                throw new IllegalArgumentException("File is not allowed to be read cause it lays outside the PWD");
            }

            content = Files.readString(path);

        } catch (Exception e) {
            content = "TOOL ERROR: " + e;
            System.out.println("[SYSTEM] Reading file:" + content);
        }

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }
}
