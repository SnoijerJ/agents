package com.example.tools;

import com.example.memory.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.tools.ToolUtils.runCommand;

public class ExecuteTestsTool implements Tool {
    @Override
    public String getName() {
        return "execute_tests";
    }

    @Override
    public String getDescription() {
        return """
            Executes the maven tests and behave tests. If the maven tests fail, the behave tests are not executed.
            Example response: {maven: {exit_code: 0, output: "Hello World", err: ""}, behave: {exit_code: 0, output: "Hello World", err: ""}}
            """;
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("execute_behave", "boolean", "When false, only the maven tests are executed. When true, both maven and behave tests are executed")
        );
    }

    @Override
    public ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger) {
        String mvnCommand = "mvn clean test -q";
        String behaveCommand = "./scripts/test";
        Map<String, String> result = new HashMap<>();

        System.out.println("[SYSTEM] Executing tests: Start execution");

        String content;
        try {
            Map<String, Object> args = new ObjectMapper().readValue(toolCall.arguments(), Map.class);
            Boolean executeBehave = (Boolean) args.get("execute_behave");

            Map<String, String> mvnResult = runCommand(mvnCommand);
            result.put("maven", mvnResult.toString());

            if ("0".equals(mvnResult.get("exit_code")) && executeBehave) {
                result.put("behave", runCommand(behaveCommand).toString());
            }
            content = result.toString();
        } catch (IOException | InterruptedException e) {
            content = "TOOL ERROR: " + e;
        }

        System.out.println("[SYSTEM] Executing tests: " + content);

        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolCall.callId())
                .output(content)
                .build();
    }
}