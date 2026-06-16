package com.example.tools;

import com.example.memory.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ShellTool implements Tool {
    @Override
    public String getName() {
        return "execute_shell";
    }

    @Override
    public String getDescription() {
        return """
            Execute a shell command on the user machine.
            This tool asks permission from the user which takes time, so prefer using other tools where possible.
            The command is executed in the following way: `/bin/bash -c "<command>"` where <command> is the input.
            You are not allowed to use this tool to get around the restrictions put by other tools.
            You are allowed to use this tool to investigate errors given by other tools.
            
            Example command: `echo "Hello World"`
            Example response: {exitcode: 0, output: "Hello World", err: ""}
            """;
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("command", "string", "The command to execute")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String content = "";
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            String command = (String) args.get("command");

            Scanner scanner = new Scanner(System.in);
            System.out.println("[SYSTEM] The agent wants to execute the following command:\n\n" + command + "\n\n[SYSTEM] Do you allow it? (yes/no [+ explanation for ai why not])");
            String answer = scanner.nextLine();
            if (!"yes".equalsIgnoreCase(answer.strip()) && !"y".equalsIgnoreCase(answer.strip())) {
                content = "TOOL ERROR: User declined command execution: " + answer;
            } else {
                content = runCommand(command).toString();
            }
        } catch (JsonProcessingException e) {
            content = "TOOL ERROR: Could not read argument: " + e;
        } catch (InterruptedException e) {
            content = "TOOL ERROR: The command execution was interrupted: " + e;
        } catch (IOException e) {
            content = "TOOL ERROR: IOException: " + e;
        }

        System.out.println("[SYSTEM] " + content.strip());

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }

    private static Map<String, String> runCommand(String command) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(new String[] {"/bin/bash", "-c", command});

        Process process = pb.start();

        Map<String, String> result = new HashMap<>();
        result.put("output", new String(process.getInputStream().readAllBytes()));
        result.put("err", new String(process.getErrorStream().readAllBytes()));
        result.put("exit_code", String.valueOf(process.waitFor()));

        return result;
    }
}
