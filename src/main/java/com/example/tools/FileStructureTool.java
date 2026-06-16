package com.example.tools;

import com.example.memory.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FileStructureTool implements Tool {
    @Override
    public String getName() {
        return "file_structure";
    }

    @Override
    public String getDescription() {
        return """
            Returns the file structure from a certain path
            Example response: {exampleRoot=[{rootDir2=[]}, {rootDir=[{rootDirDir=[{rootDirDirDir=[rootDirDirDirFile]}, rootDirDirFile]}]}, rootFile]}
            """;
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("path", "string", "Path to start searching from")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "";
        Path root;
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            root = Path.of((String) args.get("path"));

            if (!isWithinDirectory(Path.of("./"), root)) {
                content = "TOOL ERROR: The supplied path lays outside of this project";
            } else if (!Files.exists(root)) {
                content = "TOOL ERROR: The supplied path does not exist";
            } else if (Files.isRegularFile(root)) {
                content = "Supplied path is a file";
            } else {
                content = getDirectory(root);
            }
        } catch (JsonProcessingException e) {
            content = "TOOL ERROR: Could not read argument: " + e;
        }

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();

    }

    private String getDirectory(Path path) {
        if (Files.isRegularFile(path)) {
            return path.getFileName().toString();
        }
        try {
            return Map.of(path.getFileName().toString(), Files.list(path).map(this::getDirectory).toList().toString()).toString();
        } catch (IOException e) {
            return "ERROR: Could not read dir: " + e;
        }
    }

    private boolean isWithinDirectory(Path baseDir, Path target) {
        Path normalizedBase = baseDir.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();

        return normalizedTarget.startsWith(normalizedBase);
    }
}
