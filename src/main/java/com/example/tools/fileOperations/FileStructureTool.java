package com.example.tools.fileOperations;

import com.example.memory.Logger;
import com.example.tools.Parameter;
import com.example.tools.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.tools.ToolUtils.isWithinDirectory;

public class FileStructureTool implements Tool {
    @Override
    public String getName() {
        return "file_structure";
    }

    @Override
    public String getDescription() {
        return """
            Returns the file structure from a certain path
            Do not use this tool if you can retrieve your answer from the context earlier in the conversation, unless the user explicitly says the structure changed.
            Example response: {exampleRoot=[{rootDir2=[]}, {rootDir=[{rootDirDir=[{rootDirDirDir=[rootDirDirDirFile]}, rootDirDirFile]}]}, rootFile]}
            The hidden files are often not relevant and thus disabled, they can be shown by setting `include_hidden` to true, but should be false by default.
            """;
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("path", "string", "Path to start searching from"),
                new Parameter("include_hidden", "boolean", "Include the hidden folders and files. Default false")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Path root = Path.of((String) args.get("path"));
            Boolean includeHidden = (Boolean) args.get("include_hidden");

            if (!isWithinDirectory(Path.of("./"), root)) {
                content = "TOOL ERROR: File is not allowed to be accessed cause it lays outside the PWD";
            } else if (!Files.exists(root)) {
                content = "TOOL ERROR: The supplied path does not exist";
            } else if (Files.isRegularFile(root)) {
                content = "Supplied path is a file";
            } else {
                content = getDirectory(root, includeHidden);
                System.out.println("[SYSTEM] Provided file structure for " + root);
            }
        } catch (JsonProcessingException e) {
            content = "TOOL ERROR: Could not read argument: " + e;
        }

        if (content == null) {
            content = "TOOL ERROR: a path to a hidden file or folder was specified, but `include_hidden` was false";
        }

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }

    private String getDirectory(Path path, Boolean includeHidden) {
        String name = path.getFileName().toString();
        if (name.startsWith(".") && !".".equals(name) && !includeHidden) {
            return null;
        }
        if (Files.isRegularFile(path)) {
            return name;
        }
        try {
            return Map.of(name, Files.list(path)
                    .map(f -> getDirectory(f, includeHidden))
                    .filter(Objects::nonNull)
                    .toList()
                    .toString()).toString();
        } catch (IOException e) {
            return "ERROR: Could not read dir: " + e;
        }
    }
}
