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
                new Parameter("is_file", "boolean", "true to create a file, false to create a folder"),
                new Parameter("content", "string", "Content for in the file. Must be `\"\"` when `is_file` = `false`")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "success";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Path path = Path.of((String) args.get("path"));
            Boolean isFile = (Boolean) args.get("is_file");
            String fileContent = (String) args.get("content");

            if (!isWithinDirectory(Path.of("./"), path)) {
                throw new ToolException("File is not allowed to be created cause it lays outside the PWD");
            }
            if (Files.exists(path)) {
                throw new ToolException("path already exists");
            }

            Files.createDirectories(path.getParent());

            if (isFile) {
                Files.createFile(path);
                if (!"".equals(fileContent)) {
                    Files.writeString(path, fileContent);
                }
                content = "Created file: " + path;
            } else {
                if ("".equals(fileContent)) {
                    Files.createDirectory(path);
                    content = "Created directory " + path;
                } else {
                    content = "TOOL ERROR: content must be an empty string when creating a directory";
                }
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
