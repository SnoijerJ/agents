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

public class EditFileTool implements Tool {

    @Override
    public String getName() {
        return "edit_file";
    }

    @Override
    public String getDescription() {
        return """
        This tool is meant to edit a file by replacing a string by a new string. It is important that the old string is unique.
        If this is not the case, more context should be provided. There is however also an option to replace multiple occurrences if this is intentional by setting replace_all to true.
        Before using this tool, be sure to read the content of the file.
        You can write to an completely empty file by leaving `old_string` empty as well. If the file has content, old_string can not be empty.
        """;
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("path", "string", "File path of the file to edit"),
                new Parameter("old_string", "string", "The string to replace"),
                new Parameter("new_string", "string", "The string to replace the old string with"),
                new Parameter("replace_all", "boolean", "Replace all occurrences of old_string with new_string, default should be false")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "success";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Path file = Path.of((String) args.get("path"));
            String oldString = (String) args.get("old_string");
            String newString = (String) args.get("new_string");
            boolean replaceAll = (Boolean) args.get("replace_all");

            if (!isWithinDirectory(Path.of("./"), file)) {
                throw new ToolException("File is not allowed to be edited cause it lays outside the PWD");
            }
            if (!Files.exists(file)) {
                throw new ToolException("Path does not exist");
            }
            if (!Files.isRegularFile(file)) {
                throw new ToolException("Path is not a file");
            }

            String fileContent = Files.readString(file);
            if ("".equals(oldString) && !fileContent.isEmpty()) {
                throw new ToolException("old_string can not be empty if file has content");
            }
            int occurrences = countOccurrences(fileContent, oldString);
            if (occurrences == 0) {
                throw new ToolException("old_string does not occur in file");
            }
            if (occurrences > 1 && !replaceAll) {
                throw new ToolException("old_string occurs multiple times in the file. Either provide more context to make it unique, or enable replace_all");
            }

            String newFileContent = newString;
            if (!fileContent.isEmpty()) {
                newFileContent = replaceAll(fileContent, oldString, newString);
            }

            Files.writeString(file, newFileContent);

            System.out.println("[SYSTEM] Successfully edited file " + file);

        } catch (Exception e) {
            content = "TOOL ERROR: " + e;
            System.out.println("[SYSTEM] " + content);
        }

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }

    private int countOccurrences(String text, String substring) {
        if (text.isEmpty() && substring.isEmpty()) {
            return 1;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    // Helper method to replace all occurrences (literal, not regex)
    private String replaceAll(String text, String old_string, String new_string) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        int lastIndex = 0;

        while ((index = text.indexOf(old_string, lastIndex)) != -1) {
            result.append(text, lastIndex, index);
            result.append(new_string);
            lastIndex = index + old_string.length();
        }
        result.append(text.substring(lastIndex));

        return result.toString();
    }
}
