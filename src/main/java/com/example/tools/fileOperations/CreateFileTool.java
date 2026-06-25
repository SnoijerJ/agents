package com.example.tools.fileOperations;

import com.example.memory.Logger;
import com.example.tools.Parameter;
import com.example.tools.Tool;
import com.example.tools.ToolException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.example.tools.ToolUtils.isWithinDirectory;

public class CreateFileTool implements Tool {
    @Override
    public String getName() {
        return "create_file";
    }

    @Override
    public String getDescription() {
        return """
                Create a file or folder, will create parent directories if they do not exist""";
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("path", "string", "path of the file or folder to create"),
                new Parameter("is_file", "boolean", "true to create a file, false to create a folder")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "success";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Path path = Path.of((String) args.get("path"));
            Boolean isFile = (Boolean) args.get("is_file");

            if (!isWithinDirectory(Path.of("./"), path)) {
                throw new ToolException("File is not allowed to be created cause it lays outside the PWD");
            }
            if (Files.exists(path)) {
                throw new ToolException("path already exists");
            }

            Files.createDirectories(path.getParent());

            if (isFile) {
                Files.createFile(path);
                content = "Created file " + path;
            } else {
                content = "Created directory " + path;
                Files.createDirectory(path);
            }
        } catch (Exception e) {
            content = "TOOL ERROR: " + e;
        }
        System.out.println("[SYSTEM] " + content);

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }
}
