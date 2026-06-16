package com.example.tools;

import com.example.memory.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class EditFileTool implements Tool {
    @Override
    public String getName() {
        return "edit_file";
    }

    @Override
    public String getDescription() {
        return """
        Makes changes to files given an unified diff, returns `success` or `TOOL ERROR: <reason>`.
        The diff MUST be a valid GNU unified diff.
        
        The diff MUST:
        - start with `--- a/<file>`
        - contain `+++ b/<file>`
        - contain one or more `@@ -old,+new @@` hunks
        - be directly applicable with `git apply`
        
        DO NOT use:
        - "*** Begin Patch"
        - "*** Update File"
        - "*** End Patch"
        
        Example:
        
        --- a/test.txt
        +++ b/test.txt
        @@ -1,3 +1,3 @@
         Hello
        -Old
        +New
         World
        """;
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("diff", "string", "Unified diff, which can be applied with `git apply`")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "success";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            String diff = (String) args.get("diff");

            if (!diff.endsWith("\n")) {
                diff = diff + "\n";
            }

            System.out.println("[SYSTEM] Editing file");

            applyGitDiff(Path.of("./"), diff);

        } catch (Exception e) {
            content = "[SYSTEM] TOOL ERROR: " + e;
            System.out.println(content);
        }

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }

    private static void applyGitDiff(Path repoDir, String diff) throws IOException, InterruptedException {
        Path tempDiff = Files.createTempFile("agent-patch", ".diff");
        Files.writeString(tempDiff, diff);

        try {
            // 1. Validate patch first
            runGitCommand(repoDir,
                    List.of("git", "apply", "--check", tempDiff.toString()),
                    "Patch validation failed (git apply --check)");

            // 2. Optional: show stats (useful for logging/debugging)
            runGitCommand(repoDir,
                    List.of("git", "apply", "--stat", tempDiff.toString()),
                    null);

            // 3. Apply patch
            runGitCommand(repoDir,
                    List.of("git", "apply", tempDiff.toString()),
                    "Failed to apply patch");

        } finally {
            Files.deleteIfExists(tempDiff);
        }
    }

    private static void runGitCommand(Path repoDir, List<String> command, String errorMessage)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(repoDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());

        int exitCode = process.waitFor();

        if (exitCode != 0 && errorMessage != null) {
            throw new RuntimeException(
                    errorMessage + "\nCommand: " + String.join(" ", command) + "\nOutput:\n" + output
            );
        }
    }
}
