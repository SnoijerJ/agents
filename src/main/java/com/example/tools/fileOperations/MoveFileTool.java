package com.example.tools.fileOperations;

import com.example.memory.Logger;
import com.example.tools.Parameter;
import com.example.tools.Tool;
import com.example.tools.ToolException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.example.tools.ToolUtils.isWithinDirectory;

public class MoveFileTool implements Tool {
    @Override
    public String getName() {
        return "move_file";
    }

    @Override
    public String getDescription() {
        return "Moves a file or folder to a different location or rename it. If the new directory does not exist, it wil be created.";
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("current_path", "string", "The path of the file or folder that needs to be moved"),
                new Parameter("new_path", "string", "The new parent directory"),
                new Parameter("new_name", "string", "If the file or folder needs to be renamed, provide a new name. Default \"\"")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "success";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Path currentPath = Path.of((String) args.get("current_path"));
            Path newPath = Path.of((String) args.get("new_path"));
            String newName = (String) args.get("new_name");

            if (!isWithinDirectory(Path.of("./"), currentPath)) {
                throw new ToolException("File is not allowed to be moved cause it lays outside the PWD");
            }
            if (!isWithinDirectory(Path.of("./"), newPath)) {
                throw new ToolException("File is not allowed to be moved to the new location, since the new location lays outside the PWD");
            }
            if (!Files.exists(currentPath)) {
                throw new ToolException("Path does not exist");
            }
            Path target = Path.of(newPath.toString(), currentPath.getFileName().toString());
            if (!"".equals(newName)) {
                target = Path.of(newPath.toString(), newName);
            }
            if (Files.exists(target)) {
                throw new ToolException("The target path " + target + " already exists");
            }
            Files.createDirectories(target.getParent());
            Files.move(currentPath, target);
            content = "Moved file/folder from " + currentPath + " to " + target;

        } catch (IOException e) {
            content = "TOOL ERROR: " + e;
        } catch (ToolException e) {
            content = "TOOL ERROR: " + e.getMessage();
        }

        System.out.println("[SYSTEM] " + content);

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }
}
