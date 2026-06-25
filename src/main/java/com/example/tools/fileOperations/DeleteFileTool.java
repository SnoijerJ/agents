package com.example.tools.fileOperations;

import com.example.memory.Logger;
import com.example.tools.Parameter;
import com.example.tools.Tool;
import com.example.tools.ToolException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import static com.example.tools.ToolUtils.isWithinDirectory;

public class DeleteFileTool implements Tool {
    @Override
    public String getName() {
        return "delete_file";
    }

    @Override
    public String getDescription() {
        return "Deletes a file or folder. Folder can not be deleted if it contains files or other folders unless recursive is set to true";
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("path", "string", "The path of the file or folder"),
                new Parameter("recursive", "boolean", "Delete all content of folder, default false")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "success";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Path path = Path.of((String) args.get("path"));
            Boolean recursive = (Boolean) args.get("recursive");

            if (!isWithinDirectory(Path.of("./"), path)) {
                throw new ToolException("Item is not allowed to be deleted since it lays outside the pwd");
            }
            if (!Files.exists(path)) {
                throw new ToolException("path does not exist");
            }

            if (recursive) {
                deleteRecursively(path);
                content = "Deleted recursively " + path;
            } else {
                Files.delete(path);
                content = "Deleted " + path;
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

    public static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc)
                    throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
